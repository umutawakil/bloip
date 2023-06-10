package com.bloip.controllers.user

import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.domain.user.User.UserId
import com.bloip.services.LoggingService
import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.*
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
    @Autowired val loggingService: LoggingService,
    @Autowired val translationService: TranslationService
) {

    /** Join            ---------------------------- **/
    @GetMapping("/bloip-signup")
    fun signUp(
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) success: Int?,
        httpSession: HttpSession,
        model: Model
    ) : String {

        if (error != null) {
            model["error"] = error
        }
        if (success != null) {
            model["success"] = 1
        }

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "create-an-account",language)

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

        User.sendAccountConfirmationEmail(
            userId         = WebUtil.getUserIdFromSession(httpSession = httpSession)!!,
            potentialEmail = email,
            language       = httpSession.getAttribute("language") as Language
        )

        return "redirect:/bloip-signup?success=1"
    }

    @GetMapping("/complete-signup")
    fun completeSignup(
        @RequestParam(required = false) error: Int?,
        @RequestParam("t", required = true) tokenValue: String?,
        httpSession: HttpSession,
        model: Model
    ) : String {
        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "complete-account",language)

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
        model: Model,
        request: HttpServletRequest,
        response: HttpServletResponse
    ) : String {

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "account-created",language)

        return User.completeSignupFromToken(
            tokenValue = tokenValue,
            password   = password,
            success = {
                "user/signup/complete-sign-up-success"
            }
        ) as String
    }

    @GetMapping("/email-limit-reached")
    fun emailLimitReached(httpSession: HttpSession, model: Model) : String {
        val language: Language = httpSession.getAttribute("language") as Language
        model["email-limit-reached"] = translationService.getTranslationMap(context = "email-limit-reached",language)
        return "user/email-limit-reached"
    }

    /** Login            ---------------------------- **/
    @GetMapping("/bloip-login")
    fun login(
        @RequestParam(required = false) error: Int?,
        @RequestParam(required = false) passwordReset: Int?,
        request: HttpServletRequest,
        httpSession: HttpSession,
        model: Model
    ) : String {
        if (error != null) {
            model["error"] = error
        }
        if(passwordReset != null) {
            model["passwordReset"] = passwordReset
        }

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "login",language)

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

        User.logout(
            userId    = WebUtil.getUserIdFromSession(httpSession = httpSession)!!,
            deviceKey = httpSession?.getAttribute("deviceKey") as String?
        )

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
        httpSession: HttpSession,
        model: Model
    ) : String {
        if (error != null) {
            model["error"] = error
        }
        if (success != null) {
            model["success"] = success
        }

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "forgot-my-password",language)

        return "user/forgot-my-password"
    }

    @PostMapping("/bloip-forgot-my-password")
    fun processForgotMyPassword(
        @RequestParam("email", required = true) email:String,
        httpSession: HttpSession
    ) : String {

        val user: User = User.findByEmail(email = email) ?:
        return "redirect:/bloip-forgot-my-password?error=1"

        User.sendPasswordResetEmail(
            userId   = user.id,
            language = httpSession.getAttribute("language") as Language
        )
        return "redirect:/bloip-forgot-my-password?success=1"
    }

    @GetMapping("/bloip-reset-my-password")
    fun showResetMyPassword(
        @RequestParam("t", required = true) token: String,
        httpSession: HttpSession,
        model: Model
    ) : String {
        model["token"] = token

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "reset-password",language)

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
            }
        ) as String
    }

    /** Account settings   -------------------------------------------------------- **/


    /** Forgot my password---------------------------- **/
    @GetMapping("/bloip-settings")
    fun showAccountPge(
        httpSession: HttpSession,
        model: Model
    ) : String {

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "settings",language)

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

        User.showEmail(
            userId = WebUtil.getUserIdFromSession(httpSession = session)!!,
            model  = model
        )

        val language: Language = session.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "change-email",language)

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

        User.sendEmailResetEmail(
            userId            = WebUtil.getUserIdFromSession(httpSession = httpSession)!!,
            potentialNewEmail = email,
            language          = httpSession.getAttribute("language") as Language
        )

        return "redirect:/bloip-settings/email?success=1"
    }

    @GetMapping("/bloip-reset-my-email")
    fun resetEmailAddress(
        @RequestParam("t") tokenValue: String,
        response: HttpServletResponse,
        httpSession: HttpSession,
        model: Model
    ) : String {

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "email-reset",language)

        return User.changeEmailFromToken(
            tokenValue = tokenValue,
            success    = {
                model["success"] = 1
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
        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "notification-settings",language)

        val userId: UserId = WebUtil.getUserIdFromSession(httpSession = httpSession)!!

        User.showEmailStatus(userId, model)

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

        val userId: UserId = WebUtil.getUserIdFromSession(httpSession = httpSession)!!
        User.updateNotificationStatus(
            userId   = userId,
            disabled = disabled == 1,
            model    = model
        )

        return  "redirect:/bloip-settings/notifications?success=1"
    }

    /** CANSPAM compliance**/
    @GetMapping("/unsubscribe-email")
    fun disableNotificationsFromUnsubscribeEmail(
        @RequestParam("t") inputTokenValue: String,
        httpSession: HttpSession,
        model: Model
    ) : String {
        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "notification-settings",language)

        val userId: UserId = User.findUserIdFromToken(tokenValue = inputTokenValue)
        User.updateNotificationStatus(userId = userId, disabled = true, model = model)

        model["success"]  = 1

        return "user/settings/notifications"
    }

    @GetMapping("/bloip-settings/confirm-account-deletion")
    fun confirmAccountDelete(httpSession: HttpSession, model: Model) : String {
        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "confirm-account-deletion",language)

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

        val language: Language = session.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "delete-my-account",language)

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

        User.delete(userId = WebUtil.getUserIdFromSession(httpSession = httpSession)!!)

        return "redirect:/bloip-logout"
    }
}