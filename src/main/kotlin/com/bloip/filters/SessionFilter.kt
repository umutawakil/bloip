package com.bloip.filters

import com.bloip.domain.user.User
import com.bloip.helper.CookieHelper
import com.bloip.services.LoggingService
import com.bloip.services.UserService
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
 * Created by Usman Mutawakil on 6/28/22.
 */

/**TODO: Move this logic to a helper class ** */

@Component
class SessionFilter (
    @Autowired val userService: UserService,
    @Autowired val cookieHelper: CookieHelper,
    @Autowired val loggingService: LoggingService
    ): Filter {

    private val userLessMethods: List<String> = listOf(
        "get",
        "head",
        "options"
    )

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        val req: HttpServletRequest = request as HttpServletRequest
        val res:HttpServletResponse = response as HttpServletResponse
        loggingService.log("URL: " + req.requestURL.toString())

        //TODO: Temporary
        res.addHeader("Cache-Control", "no-store")

        if (isValidUserSession(request = req)) {
            chain.doFilter(request, response)
            return
        }

        /**
         * Everyone gets a session and the only visitors that have a User object are those that have made a post
         * literally using the POST http method. So get/head/options requests can ass through. The userId only gets
         * set if the visitor has a user or if the visitor needs a user to be created because they are performing a POST.
         */
        val session: HttpSession = req.getSession(false) ?: req.getSession(true)
        val user: User? = cookieHelper.getUserFromCookie(req) ?: if (!userLessMethods.contains(req.method.lowercase())) { userService.createNewUser()} else {null}
        if(user != null) {
            session.setAttribute("userId", user.id)
            cookieHelper.resetCookie(user, req,  res)
        }
        chain.doFilter(request, response)
    }

    fun isValidUserSession(request: HttpServletRequest) : Boolean {
        var session: HttpSession = request.getSession(false)?: return false
        return session.getAttribute("userId") != null
    }
}