package com.bloip.controllers.user

import com.bloip.controllers.user.helpers.LoginSuccessHandler
import com.bloip.domain.user.User
import com.bloip.helper.CookieHelper
import com.bloip.services.LoggingService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
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
    @Autowired val cookieHelper: CookieHelper,
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
        @RequestParam("email") email: String,
        httpSession: HttpSession
    ) : String {
        if (User.usernameExists(username = email)) {
            return "redirect:/bloip-signup?error=1"
        }
        User.findById(
            WebUtil.getUserIdFromSession(httpSession = httpSession)!!

        )!!.sendAccountConfirmationEmail(potentialEmail = email)

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

        return User.showCompleteSignupView(
            model      = model,
            inputToken = tokenValue!!,
            onError    = {
                "redirect:/complete-signup?error=1"
            },
            onSuccess  = {
                "user/signup/complete-sign-up"
            }) as String
    }

    @PostMapping("/complete-signup")
    fun processSignup(
        @RequestParam("t", required = true) tokenValue: String,
        @RequestParam(required = true) password: String,
        httpSession: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) : String {
        return User.completeSignupFromToken(
            tokenValue = tokenValue,
            password   = password,
            success = {
                "user/signup/complete-sign-up-success"
            },
            failure = {
                "redirect:/complete-signup?error=1"
            }
        ) as String
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

        /** This worked for a while then request.cookies starting returning null in certain contexts **/
        if (request.cookies != null) {
            for (cookie in request.cookies) {
                val cookieName = cookie.name
                val cookieToDelete = Cookie(cookieName, null)
                cookieToDelete.maxAge = 0
                response.addCookie(cookieToDelete)
            }
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
        @RequestParam("email", required = true) email:String,
        httpSession: HttpSession
    ) : String {

        val user: User = User.findByEmail(email = email) ?:
        return "redirect:/bloip-forgot-my-password?error=1"

        user.sendPasswordResetEmail()
        return "redirect:/bloip-forgot-my-password?success=1"
    }

    @GetMapping("/bloip-reset-my-password")
    fun showResetMyPassword(
        @RequestParam("t", required = true) token: String,
        model: Model
    ) : String {
        if (User.findByToken(tokenValue = token) == null) {
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

        return User.changePasswordFromToken(
            tokenValue = tokenValue,
            password   = password,
            success    = {
                "redirect:/bloip-login?passwordReset=1"
            },
            failure    = {
                response.sendError(400)
                model["message"] = "Email address doesn't exist or reset email expired: $tokenValue"
                "error.html"
            }
        ) as String
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

        model["email"] = User.findById(
            WebUtil.getUserIdFromSession(httpSession = session)!!
        )!!.getEmail()!!

        return "user/settings/email/email"
    }

    @PostMapping("/bloip-settings/email")
    fun processEmailChangeRequest(
        request: HttpServletRequest,
        response: HttpServletResponse,
        httpSession: HttpSession,
        @RequestParam("newEmail") email: String,
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

        /** Verify email address is not in use **/
        if (User.usernameExists(username = email)) {
            return "redirect:/bloip-settings/email?error=1"
        }

        val user: User = User.findById(WebUtil.getUserIdFromSession(httpSession = httpSession)!!)!!
        user.sendEmailResetEmail(potentialNewEmail = email)

        return "redirect:/bloip-settings/email?success=1"
    }

    @GetMapping("/bloip-reset-my-email")
    fun resetEmailAddress(
        @RequestParam("t") tokenValue: String,
        response: HttpServletResponse,
        model: Model
    ) : String {
        return User.changeEmailFromToken(
            tokenValue = tokenValue,
            success = {
                model["success"] = 1
                "user/settings/email/reset-email.html"
            },
            failure = {
                model["error"] = 1
                "user/settings/email/reset-email.html"
            }
        ) as String
    }

    @GetMapping("/bloip-settings/notifications")
    fun showNotificationSettings(
        httpSession: HttpSession,
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) success: Int?,
        model: Model
    ) : String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession = httpSession)!!
        val user: User = User.findById(userId = userId)!!

        user.showIfDisabled(model)
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
        User.findById(userId = userId)!!.
        updateNotificationStatus(disabled = (disabled == 1)).
        showIfDisabled(model)

        return  "redirect:/bloip-settings/notifications?success=1"
    }

    /** CANSPAM compliance**/
    @GetMapping("/unsubscribe-email")
    fun disableNotificationsFromUnsubscribeEmail(
        @RequestParam("t") inputTokenValue: String,
        model: Model
    ) : String {
        //TODO: Needs to handle expired or missing token from old email
        println("Trying token!!")
        val user: User? = User.findByToken(tokenValue = inputTokenValue)
        if (user == null) {
            println("No user found for token")
            throw RuntimeException("Expired email!!!")
        }
        println("User found!!!")
        user.updateNotificationStatus(disabled = true).
        showIfDisabled(model)

        model["success"]  = 1

        println("Success!!!")

        return "user/settings/notifications"
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

        User.findById(
            userId      = WebUtil.getUserIdFromSession(
            httpSession = httpSession
            )!!
        )!!.delete()

        return "redirect:/bloip-logout"
    }
}