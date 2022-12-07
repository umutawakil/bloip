package com.bloip.helper

import com.bloip.domain.User
import com.bloip.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
@Component
class CookieHelper(
    @Autowired val userService: UserService
) {
    private val RME_COOKIE_NAME: String = "rme"

    fun getUserFromCookie(request: HttpServletRequest) : User? {
        val cookie = findCookieByName(RME_COOKIE_NAME, request.cookies) ?: return null
        return userService.findByCookieCode(cookie.value)
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

    fun deleteExistingRMECookiesFromResponse(cookies: Array<Cookie>?, response: HttpServletResponse) {
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