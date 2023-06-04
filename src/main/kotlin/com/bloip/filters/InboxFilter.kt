package com.bloip.filters

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.user.User
import org.springframework.stereotype.Component
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/15/22.
 */
@Component
class InboxFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpSession: HttpSession? = (request as HttpServletRequest).getSession(false)
        if(httpSession == null) {
            println("Error: User has no session but is hitting inbox filter")
            throw RuntimeException("Error: User has no session but is hitting inbox filter")
        }
        val userId: User.UserId? = httpSession.getAttribute("userId") as User.UserId?

        if (userId == null) {
            httpSession.setAttribute("inboxTotal", 0)
            chain.doFilter(request, response)
            return
        }
        //TODO: Whats the best way to unit/integration test this?
        Discussion.setInboxTotalInSession(userId = userId, httpSession = httpSession)

        chain.doFilter(request, response)
    }
}