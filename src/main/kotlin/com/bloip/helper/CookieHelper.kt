package com.bloip.helper

import com.bloip.domain.user.User
import org.springframework.stereotype.Component
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
@Component
class CookieHelper {
    private val RME_COOKIE_NAME: String = "rme"

    fun getUserIdFromCookie(request: HttpServletRequest) : User.UserId? {
        val cookie = findCookieByName(RME_COOKIE_NAME, request.cookies) ?: return null
        return User.getUserIdFromJwt(tokenValue = cookie.value)
    }

    fun resetCookie(userId: User.UserId, request: HttpServletRequest, response: HttpServletResponse) {
        deleteExistingRMECookiesFromResponse(
            cookies  = request.cookies,
            response = response
        )

        val code: String  = User.createUserIdJwt(userId)
        val cookie        = Cookie(RME_COOKIE_NAME, code)
        cookie.secure     = true
        cookie.isHttpOnly = true
        cookie.path       = "/"
        cookie.domain     = getDomain(request)
        cookie.maxAge     = 60 * 60 * 24 * 365 * 10 // 10 year cookie

        response.addCookie(cookie)
    }

    private fun findCookieByName(name: String, cookies: Array<Cookie>?): Cookie? {
        if(cookies == null) {
            return null
        }
        for (c in cookies) {
            if(c.name.equals(name)) {//TODO: Needs to enforce unique keys or check them all or take the newest an delete the rest or something
                return c
            }
        }
        return null
    }

    private fun getDomain(request: HttpServletRequest) : String {
        return request.serverName.replace(".*\\.(?=.*\\.)", "")
    }

    private fun deleteExistingRMECookiesFromResponse(cookies: Array<Cookie>?, response: HttpServletResponse) {
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