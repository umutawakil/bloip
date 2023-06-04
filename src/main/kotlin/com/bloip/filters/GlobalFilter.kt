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
            UPLOAD_COMPLETE_URL, "/bloip-bind-device"
        )

        /** Ensure they are using HTTPS before we start messing with them. This header represents the viewers' protocol.
         * All communication between the EC2 instance this code runs on and the ELB is in HTTPS and between the ELB and cloudfront
         * is also in HTTPS but communication between the viewer and cloudfront may be in HTTP so this send a redirect request
         * to the user for the same URL they requested but in HTTPS. If we dont check this header the protocol in the request
         * will always be HTTPS. **/
        val proxiedProtocol: String? = req.getHeader("X-Forwarded-Proto")
        if (proxiedProtocol != null && proxiedProtocol.lowercase().trim() == "http") {
            response.sendRedirect(req.requestURL.toString().replace("http://","https://", ignoreCase = true))
            return
        }

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
                            " Host must match origin. Path: ${req.servletPath}, Host: $host, Origin: $origin"
                )
                loggingService.securityLog("CSRF Request attempted -> Path: ${req.servletPath}, Host: $host, Origin: $origin")
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