package com.bloip.filters

import com.bloip.controllers.discussion.UploadController.Companion.UPLOAD_COMPLETE_URL
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/28/22.
 */
@Component
class GlobalFilter(
    @Autowired private val loggingService: LoggingService
): Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req: HttpServletRequest  = request as HttpServletRequest
        val res: HttpServletResponse = response as HttpServletResponse
        val requestMethod: String    = request.method.lowercase()

        val pathExceptions: Set<String> = setOf(
            UPLOAD_COMPLETE_URL
        )
        /** Reject clearly bogus requests from outside this domain
         * Also make host and origin mandatory.
         * **/
        if (requestMethod == "post" && !pathExceptions.contains(req.servletPath)) {
            val host: String?   = request.getHeader("Host")
            val origin: String? = request.getHeader("Origin")?.replace("https://","") //TODO: Probably a performance hit to use URL class here for every request

            if ((host != origin) || (host == null) || (origin == null)) {
                response.sendError(
                    400,
                    "Bad request: Your browser is not sending required security information.\r\n" +
                            " Host must match origin. Host: $host, Origin: $origin"
                )
                loggingService.securityLog("CSRF Request attempted -> Host: $host, Origin: $origin")
                return
            }
        }

        /** Prevent the site from being run in iframes. This is only really for the cases in which
         * an attacker is loading the site in an iframe for malicious purposes such as click-jacking or some sort
         * of attempt to bypass existing CSRF safety measures.
         * **/
        res.addHeader("Content-Security-Policy","frame-ancestors 'none'")
        res.addHeader("X-Frame-Options", "DENY")

        /** Save the non-login Url the user is targeting. At the moment seems bloip-settings and inbox would be the only ones **/
        val session: HttpSession = request.getSession(false) ?: request.getSession(true)
        val desiredUrl: String = request.servletPath
        if (
            (requestMethod == "get") &&
            (!desiredUrl.contains("login")) &&
            (!desiredUrl.contains("logout")) &&
            (!desiredUrl.contains("reset-my-password")) &&
            (!desiredUrl.contains("forgot-my-password"))
        ) {
            session.setAttribute("desiredUrl", desiredUrl)
        }

        chain.doFilter(request, res)
    }
}