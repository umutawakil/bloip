package com.bloip.controllers.user

import com.bloip.controllers.user.helpers.LoginSuccessHandler
import com.bloip.domain.value.EmailAddress
import com.bloip.domain.Token
import com.bloip.domain.user.User
import com.bloip.domain.user.authentication.AuthenticationUserDetail
import com.bloip.helper.CookieHelper
import com.bloip.services.EmailService
import com.bloip.services.LoggingService
import com.bloip.services.UserService
import com.bloip.services.UserTokenService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import java.util.*
import javax.annotation.PostConstruct
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/22/22.
 *
 * TODO: Needs functional tests and CSRF checks as well as alarms for unusual failures in this domain
 * since users are first class citizens in this system.
 */
@Controller
class UserController(
    @Autowired val userService: UserService,
    @Autowired val cookieHelper: CookieHelper,
    @Autowired val passwordEncoder: PasswordEncoder,
    @Autowired val emailService: EmailService,
    @Autowired val userTokenService: UserTokenService,
    @Autowired val loginSuccessHandler: LoginSuccessHandler,
    @Autowired val loggingService: LoggingService
) {
    @PostConstruct
    fun init() {
        loginSuccessHandler.cookieHelper = cookieHelper /** This is done to prevent a cyclical dependency on bean init **/
    }

    /** Join            ---------------------------- **/
    @GetMapping("/bloip-signup")
    fun signUp(
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) success: Int?,
        model: Model
    ) : String {

        if (error != null) {
            model["error"] = error
        }
        if (success != null) {
            model["success"] = 1
        }
        return "user/signup/signup"
    }

    @PostMapping("/account-confirmation-email")
    fun processSignupRequest(
        @RequestParam("email") inputEmail: String
    ) : String {
        val email = EmailAddress(inputEmail)

        if (userService.usernameExists(username = email.value)) {
            return "redirect:/bloip-signup?error=1"
        }
        val tokenResult: UserTokenService.TokenResult = userTokenService.generateUserAccountToken(user = null, email = email)
        if(tokenResult.limitReached) {
            return "redirect:/email-limit-reached"
        }

        emailService.sendAccountConfirmationToken(
            token = tokenResult.token!!.value,
            toAddress = email
        )

        return "redirect:/bloip-signup?success=1"
    }

    @GetMapping("/complete-signup")
    fun completeSignup(
        @RequestParam(required = false) error: Int?,
        @RequestParam("t", required = true) tokenValue: String?,
        model: Model
    ) : String {
        if(error != null) {
            model["error"] = error
            return "user/signup/complete-sign-up"
        }

        val token: Token? = userTokenService.getToken(tokenValue)
        if (token == null || tokenValue == null) {
            return "redirect:/complete-signup?error=1"
        }
        model["email"] = token.email
        model["token"] = tokenValue

        return "user/signup/complete-sign-up"
    }

    @PostMapping("/complete-signup")
    fun processSignup(
        @RequestParam("t", required = true) tokenValue: String?,
        @RequestParam(required = true) password: String,
        httpSession: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) : String {
        val token: Token? = userTokenService.getToken(tokenValue)
        if (token == null || tokenValue == null) {
            return "redirect:/complete-signup?error=1"
        }

        if (userService.usernameExists(username = token.email)) {
            return "redirect:/complete-signup?error=2"
        }
        if (password.length > 20 || password.length < 8) { //Should only happen on headless submissions
            throw RuntimeException("Bad password")
        }

        val user: User = getOrCreateUser(httpSession)
        cookieHelper.resetCookie(user, request, response)

        user.authenticationUserDetail = AuthenticationUserDetail(
            user     = user,
            email    = EmailAddress(token.email),
            password = passwordEncoder.encode(password)
        )
        userService.save(user)
        userTokenService.remove(token = token)

        return "user/signup/complete-sign-up-success"
    }

    fun getOrCreateUser(httpSession: HttpSession) : User {
        val userId: Long? = httpSession.getAttribute("userId") as Long?
        if(userId != null) return userService.findById(userId = userId)!!

        return userService.createNewUser()
    }

    @GetMapping("/email-limit-reached")
    fun emailLimitReached() : String {
        return "user/email-limit-reached"
    }

    /** Login            ---------------------------- **/
    @GetMapping("/bloip-login")
    fun login(
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) passwordReset: Int?,
        request: HttpServletRequest,
        model: Model
    ) : String {
        if (error != null) {
            model["error"] = error
        }
        if(passwordReset != null) {
            model["passwordReset"] = passwordReset
        }

        return "user/login"
    }

    /** Logout            ---------------------------- **/
    @GetMapping("/bloip-logout")
    fun logout(
        @RequestParam(required = false) url: String?,
        httpSession: HttpSession?,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) : String {
        //httpSession?.invalidate()

        httpSession?.removeAttribute("userId")

        SecurityContextHolder.clearContext()

        for (cookie in request.cookies) {
            val cookieName = cookie.name
            val cookieToDelete = Cookie(cookieName, null)
            cookieToDelete.maxAge = 0
            response.addCookie(cookieToDelete)
        }
        request.logout()

        return "redirect:/logout"
    }

    /** Forgot my password---------------------------- **/
    @GetMapping("/bloip-forgot-my-password")
    fun showForgotMyPassword(
        @RequestParam(required = false) error:Int?,
        @RequestParam(required = false) success: Int?,
        model: Model
    ) : String {
        if (error != null) {
            model["error"] = error
        }
        if (success != null) {
            model["success"] = success
        }
        return "user/forgot-my-password"
    }

    @PostMapping("/bloip-forgot-my-password")
    fun processForgotMyPassword(
        @RequestParam("email", required = true) inputEmail:String,
        httpSession: HttpSession
    ) : String {
        val email = EmailAddress(inputEmail)
        if (!userService.usernameExists(username = email.value)) {
            return "redirect:/bloip-forgot-my-password?error=1"
        }
        val tokenResult: UserTokenService.TokenResult = userTokenService.generateUserAccountToken(
            user  = null,
            email = email
        )
        if (tokenResult.limitReached) {
            return "redirect:/email-limit-reached"
        }

        emailService.sendPasswordResetToken(
            token     = tokenResult.token!!.value,
            toAddress = email
        )
        return "redirect:/bloip-forgot-my-password?success=1"
    }

    @GetMapping("/bloip-reset-my-password")
    fun showResetMyPassword(
        @RequestParam("t", required = true) token: String,
        model: Model
    ) : String {
        if (userTokenService.getToken(token) == null) {
            model["error"] = 1
        }
        model["token"] = token

        return "user/reset-my-password"
    }
    @PostMapping("/bloip-reset-my-password")
    fun processResetMyPassword(
        @RequestParam("t", required = true) tokenValue: String,
        @RequestParam(required = true) password: String,
        model: Model,
        response: HttpServletResponse
    ) : String {
        val token: Token? = userTokenService.getToken(tokenValue)
        if (token == null || !userService.usernameExists(token.email)) {
            response.sendError(400)
            model["message"] = "Email address doesn't exist or reset email expired: ${token?.email}"
            return "error.html"
        }
        if (password.length < 8 || password.length > 20) {
            model["message"] = "Password bounds are invalid"
            response.sendError(400)
            return "error.html"
        }

        var user: User = userService.findByUsername(username = token.email)!!
        user.authenticationUserDetail!!.password = passwordEncoder.encode(password)

        userTokenService.remove(token)

        return "redirect:/bloip-login?passwordReset=1"
    }

    /** Account settings   -------------------------------------------------------- **/


    /** Forgot my password---------------------------- **/
    @GetMapping("/bloip-settings")
    fun showAccountPge(
        model: Model
    ) : String {
        return "user/settings/settings"
    }

    @GetMapping("/bloip-settings/email")
    fun showEmailSettings(
        model: Model,
        session: HttpSession,
        success: Int?,
        error: Int?
    ) : String {
        if (success != null) {
            model["success"] = success
        }
        if (error != null) {
            model["error"] = error
        }

        val csrfToken: String = UUID.randomUUID().toString()
        model["csrfToken"] = csrfToken
        session.setAttribute("csrfToken", csrfToken)

        model["email"] = userService.findById(
            WebUtil.getUserIdFromSession(httpSession = session)!!
        )!!.getEmail()!!

        return "user/settings/email/email"
    }

    @PostMapping("/bloip-settings/email")
    fun processEmailChangeRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        httpSession: HttpSession,
        @RequestParam("newEmail") inputNewEmail: String,
        @RequestParam("csrfToken", required = true) csrfToken: String,
        model: Model
    ) : String {
        if (csrfToken != httpSession.getAttribute("csrfToken")) {
            loggingService.securityLog(
                "Bad csrfToken submitted. t1: ${csrfToken},\r\n t2: ${httpSession.getAttribute("csrfToken")}"
            )
            response.sendError(400)
            return "error"
        }
        httpSession.removeAttribute("csrfToken")
        val newEmail = EmailAddress(inputNewEmail)

        /** Verify email address is not in use **/
        if (userService.usernameExists(username = newEmail.value)) {
            return "redirect:/bloip-settings/email?error=1"
        }

        val user: User = userService.findById(WebUtil.getUserIdFromSession(httpSession = httpSession)!!)!!

        val tokenResult: UserTokenService.TokenResult = userTokenService.generateUserAccountToken(
            user  = user,
            email = newEmail
        )
        if (tokenResult.limitReached) {
            return "redirect:/email-limit-reached"
        }


        emailService.sendEmailResetToken(token = tokenResult.token!!.value, toAddress = newEmail)


        return "redirect:/bloip-settings/email?success=1"
    }

    @GetMapping("/bloip-reset-my-email")
    fun resetEmailAddress(
        @RequestParam("t") tokenValue: String,
        response: HttpServletResponse,
        model: Model
    ) : String {
        val token: Token? = userTokenService.getToken(tokenValue)
        if (token == null) {
            model["error"] = 1
            return "user/settings/email/reset-email.html"
        }

        val user: User = userService.findById(token.user!!.id)!!
        if(!userService.usernameExists(token.email)) {
            //user.authenticationUserDetail!!.setEmailAddress(emailAddress = EmailAddress(token.email))
            userService.updateEmail(user, newEmail = EmailAddress(token.email))
            userTokenService.remove(token = token)
            model["success"] = 1
        } else {
            return "redirect:/bloip-settings/email?error=1"
        }

        return "user/settings/email/reset-email.html"
    }

    @GetMapping("/bloip-settings/notifications")
    fun showNotificationSettings(
        httpSession: HttpSession,
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) success: Int?,
        model: Model
    ) : String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession = httpSession)!!
        val user: User = userService.findById(userId = userId)!!

        model["disabled"] = user.emailDisabled
        if(success != null) {
            model["success"] = success
        }

        val csrfToken: String = UUID.randomUUID().toString()
        model["csrfToken"] = csrfToken
        httpSession.setAttribute("csrfToken", csrfToken)

        return "user/settings/notifications"
    }

    @PostMapping("/bloip-settings/notifications")
    fun processNotificationSettings(
        @RequestParam disabled: Int?,
        httpSession: HttpSession,
        response: HttpServletResponse,
        model: Model,
        @RequestParam csrfToken: String
    ) : String {
        if (csrfToken != httpSession.getAttribute("csrfToken")) {
            response.sendError(400)
            return "error"
        }
        httpSession.removeAttribute("csrfToken")

        val userId: Long = WebUtil.getUserIdFromSession(httpSession = httpSession)!!
        val user: User = userService.findById(userId = userId)!!
        user.emailDisabled = (disabled == 1)
        val updatedUser = userService.save(user = user)

        model["disabled"] = updatedUser.emailDisabled

        return  "redirect:/bloip-settings/notifications?success=1"
    }

    /** CANSPAM compliance**/
    @GetMapping("/unsubscribe-email")
    fun disableNotificationsFromUnsubscribeEmail(
        @RequestParam("t") token: String,
        model: Model
    ) : String {

        var token: Token? = userTokenService.getToken(token = token)
        if (token != null) {
            token.user!!.emailDisabled = true
            val user: User = userService.save(user = token.user!!)
            model["success"]  = 1
            model["disabled"] = user.emailDisabled

            return "user/settings/notifications"

        } else {
            return "redirect:/bloip-settings/notifications?success=1"
        }
    }

    @GetMapping("/bloip-settings/confirm-account-deletion")
    fun confirmAccountDelete() : String {
        return "user/settings/confirm-account-deletion"
    }

    @GetMapping("/bloip-settings/delete-my-account")
    fun showDeleteMyAccount(
        model: Model,
        session: HttpSession
    ) : String {
        val csrfToken: String = UUID.randomUUID().toString()
        model["csrfToken"] = csrfToken
        session.setAttribute("csrfToken", csrfToken)

        return "user/settings/delete-my-account"
    }

    @PostMapping("/bloip-settings/delete-my-account")
    fun processDeleteMyAccount(
        httpSession: HttpSession,
        @RequestParam csrfToken: String,
        response: HttpServletResponse
    ) : String {
        if (csrfToken != httpSession.getAttribute("csrfToken")) {
            response.sendError(400)
            return "error"
        }
        httpSession.removeAttribute("csrfToken")

        userService.delete(userId = WebUtil.getUserIdFromSession(httpSession = httpSession)!!)
        return "redirect:/bloip-logout"
    }
}