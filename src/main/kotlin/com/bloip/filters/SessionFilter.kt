package com.bloip.filters

import com.bloip.domain.User
import com.bloip.services.LoggingService
import com.bloip.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.Cookie
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
        @Autowired val loggingService: LoggingService
    ): Filter {

    private val RME_COOKIE_NAME: String = "rme"

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
            var user = getUserFromCookie(req)
            if(user != null) {
                val session: HttpSession = req.getSession(false) ?: req.getSession(true)
                session.setAttribute("userId", user.id)
                resetCookie(user, req,  res)
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
        var user = getUserFromCookie(req)
        if(user == null) {
            user = createNewUser()
            //loggingService.log("New user being created: ${user.id}");
        } else {
           // loggingService.log("User located in remember me cookie: " + user.id);
        }
        session.setAttribute("userId", user.id)
        resetCookie(user, req,  res)
    }

    fun getUserFromCookie(request: HttpServletRequest) : User? {
        val cookie = findCookieByName(RME_COOKIE_NAME, request.cookies) ?: return null
        return userService.findByCookieCode(cookie.value)
    }

    fun createNewUser() : User {
        return userService.createNewUser()
    }

    fun findCookieByName(name: String, cookies: Array<Cookie>?): Cookie? {
        if(cookies == null) {
            return null
        }

        //loggingService.log("Cookies: ${cookies.size}")

        for (c in cookies) {
           // println("cookie name: " + c.name +", value: ${c.value}")
            if(c.name.equals(name)) {//TODO: Needs to enforce unique keys or check them all or take the newest an delete the rest or something
                return c
            }
        }
        //println("No cookie found what the?")
        //RuntimeException("No cookie found").printStackTrace()
        return null
    }

    fun resetCookie(user: User, request: HttpServletRequest, response: HttpServletResponse) {
        //loggingService.log("Resetting new cookie and deleting old ones...")
        deleteExistingRMECookiesFromResponse(cookies = request.cookies, response = response)

        val code: String = UUID.randomUUID().toString()
        val cookie = Cookie(RME_COOKIE_NAME, code)
        cookie.secure = true
        cookie.isHttpOnly = true
        cookie.path = "/"
        cookie.domain = getDomain(request)
        cookie.maxAge = 60 * 60 * 24 * 365 * 10 // 10 year cookie

        response.addCookie(cookie)
        userService.resetCookies(user, code, request.remoteAddr)
    }

    fun getDomain(request: HttpServletRequest) : String {
        val domain: String =  request.serverName.replace(".*\\.(?=.*\\.)", "")
         return domain
    }

    fun deleteExistingRMECookiesFromResponse(cookies: Array<Cookie>?,response: HttpServletResponse) {
        if(cookies == null) {
            return
        }
        for (c in cookies) {
            if(c.name.equals(RME_COOKIE_NAME)) {
                c.value = ""
                c.maxAge = 0
                response.addCookie(c)
            }
        }
    }
}