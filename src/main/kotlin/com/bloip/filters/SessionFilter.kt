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
    @Autowired val userService: UserService, @Autowired val loggingService: LoggingService): Filter {

    private val RME_COOKIE_NAME: String = "rme"

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val req: HttpServletRequest = request as HttpServletRequest
        val res:HttpServletResponse = response as HttpServletResponse

        val requestUrl: String = req.requestURL.toString()
        loggingService.log("RequestURL: ${requestUrl}")

        var session: HttpSession? = req.getSession(false)
        if (session == null && !requestUrl.contains(".")) {
            handleNoSession(req, res)
        } else {
            loggingService.log("User already has a session or resource desired is not session protected. Session creation logic skipped: " + requestUrl)
        }
        chain!!.doFilter(request, response);
    }

    @Transactional
    fun handleNoSession(req: HttpServletRequest, res: HttpServletResponse) {
        loggingService.log("No session found");
        val session: HttpSession = req.getSession(true)

        //Check for cookie, if it exists use it to get userId or create a new user and use that usrId
        var user = getUserFromCookie(req)
        if(user == null) {
            user = createNewUser()
            loggingService.log("New user being created: ${user.id}");
        } else {
            loggingService.log("User located in remember me cookie: " + user.id);
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

        loggingService.log("Cookies: ${cookies.size}")

        for (c in cookies) {
            if(c.name.equals(name)) {//TODO: Needs to enforce unique keys or check them all or take the newest an delete the rest or something
                return c
            }
        }
        return null
    }

    fun resetCookie(user: User, request: HttpServletRequest, response: HttpServletResponse) {
        loggingService.log("Resetting new cookie and deleting old ones...")
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
        return request.serverName.replace(".*\\.(?=.*\\.)", "")
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