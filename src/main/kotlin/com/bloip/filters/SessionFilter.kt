package com.bloip.filters

import com.bloip.helper.CookieHelper
import com.bloip.services.LoggingService
import com.bloip.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/28/22.
 */

/**TODO: Move this logic to a helper class ** */

@Component
class SessionFilter (
    @Autowired val userService: UserService,
    @Autowired val cookieHelper: CookieHelper,
    @Autowired val loggingService: LoggingService
    ): Filter {

    private val sessionRequiredURLs:List<String> = listOf(
        "/inbox"
    )

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        val req: HttpServletRequest = request as HttpServletRequest
        val res:HttpServletResponse = response as HttpServletResponse

        loggingService.log("URL: " + req.requestURL.toString())

        /** Ensure they are using https before we start messing with them **/
        val proxiedProtocol: String? = req.getHeader("X-Forwarded-Proto")
        if (proxiedProtocol != null && proxiedProtocol.lowercase().trim() == "http") {
            response.sendRedirect(req.requestURL.toString().replace("http://","https://", ignoreCase = true))
            return
        }

        //TODO: Temporary
        res.addHeader("Cache-Control", "no-store")
        val requestUrl: String = req.requestURL.toString()

        if (isValidUserSession(request = req)) {
            chain.doFilter(request, response)
            return
        }

        /** This request is not a request to change something therefore we don't need a user **/
        if(req.method.lowercase() == "get") {
            /** If a user has a cookie because they've posted before then load it otherwise allow them through. **/
            var user = cookieHelper.getUserFromCookie(req)
            if(user != null) {
                val session: HttpSession = req.getSession(false) ?: req.getSession(true)
                session.setAttribute("userId", user.id)
                cookieHelper.resetCookie(user, req,  res)
            }

            /** No user is found in the session or cookie and they are accessing a user specific context like inbox.
             * send them home **/
            if(user == null && pathRequiresSession(path = requestUrl)) {
                res.sendRedirect("/")
                return
            }

            chain.doFilter(request, response)
            return
        }

        /** This should only handle POST requests from here on out: "Maybe headrequests?" **/
        if(req.method.lowercase() != "post") {
            throw RuntimeException("Unsupported request")
        }

        /** Visitor has no user in its session (may have a session that only contains localization info) but they are about
         * invoke a command that requires a session.**/
        handleUserlessSession(req, res)
        chain.doFilter(request, response);
    }

    fun pathRequiresSession(path: String) : Boolean {
        for(requiredPath in sessionRequiredURLs) {
            if(path.contains(requiredPath)) {
                return true
            }
        }
        return false
    }

    fun isValidUserSession(request: HttpServletRequest) : Boolean {
        var session: HttpSession = request.getSession(false)?: return false
        return session.getAttribute("userId") != null
    }

    /** A visitor may have a session holding localization info but no user data which is required to make POST requests **/
    @Transactional
    fun handleUserlessSession(req: HttpServletRequest, res: HttpServletResponse) {
       loggingService.debug("No session found");
        val session: HttpSession = req.getSession(false) ?: req.getSession(true)

        //Check for cookie, if it exists use it to get userId or create a new user and use that usrId
        var user = cookieHelper.getUserFromCookie(req)
        if(user == null) {
            user = userService.createNewUser()
            //loggingService.log("New user being created: ${user.id}");
        } else {
           // loggingService.log("User located in remember me cookie: " + user.id);
        }
        session.setAttribute("userId", user.id)
        cookieHelper.resetCookie(user, req,  res)
    }
}