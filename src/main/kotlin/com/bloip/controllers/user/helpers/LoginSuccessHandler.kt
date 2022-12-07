package com.bloip.controllers.user.helpers

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.authentication.AuthenticationUserDetail
import com.bloip.helper.CookieHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/28/22.
 */
@Component
class LoginSuccessHandler(
    @Autowired val applicationProperties: ApplicationProperties
) : SimpleUrlAuthenticationSuccessHandler() {
    var cookieHelper: CookieHelper? = null

    override fun onAuthenticationSuccess(
        request:        HttpServletRequest,
        response:       HttpServletResponse,
        authentication: Authentication
    ) {
        val authenticationUserDetail: AuthenticationUserDetail = authentication.principal as AuthenticationUserDetail
        val httpSession: HttpSession = request.getSession(false)
        httpSession.setAttribute("userId", authenticationUserDetail.user.id)

        cookieHelper!!.resetCookie(
            user     = authenticationUserDetail.user,
            request  = request,
            response = response
        )
        httpSession.setAttribute("loginDate", Date())

        /** Apparently the SimpleUrlAuthenticationSuccessHandler does this automatically (redirecting to target url before
         * authentication) but that doesn't seem to work perhaps because I'm overriding the success handler method as opposed
         * to letting this run by default in the HttpSecurity configuration in the SecurityConfig class (At the time of this writing)
         */
        val desiredUrl: String? = httpSession.getAttribute("desiredUrl") as String?
        if (desiredUrl != null) {
            response.sendRedirect(desiredUrl)
            httpSession.removeAttribute("desiredUrl")
            return
        }
        response.sendRedirect("/")
    }
}