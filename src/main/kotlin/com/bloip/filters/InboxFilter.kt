package com.bloip.filters

import com.bloip.services.InboxService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
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
class InboxFilter(
    @Autowired val inboxService: InboxService
) : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {

        val httpSession: HttpSession = (request as HttpServletRequest).getSession(false)
        if (httpSession.getAttribute("userId") == null) {
            httpSession.setAttribute("inboxTotal", 0)
            chain.doFilter(request, response)
            return
        }

        //TODO: Whats the best way to unit/integration test this?
        httpSession.setAttribute(
            "inboxTotal",
            inboxService.getInboxTotal(
                userId = WebUtil.getUserIdFromSession(httpSession)!!
            )
        )
        chain.doFilter(request, response)
    }
}