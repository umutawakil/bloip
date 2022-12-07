package com.bloip.filters

import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 11/27/22.
 */

/** TODO Needs work **/
@Component
class SettingsFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req: HttpServletRequest  = request as HttpServletRequest
        val res: HttpServletResponse = response as HttpServletResponse
        val loginDate: Date?         = getLoginDate(req)

        if (lastLoginTimeIsGreaterThanMaxAllowed(loginDate, maxInMinutes = 5)) {
            res.sendRedirect(
                "/bloip-logout"
            )
            return
        }
        chain.doFilter(request, response)
    }

    private fun getLoginDate(request: HttpServletRequest) : Date? {
        return request.getSession(false)?.getAttribute("loginDate") as Date?
    }

    private fun lastLoginTimeIsGreaterThanMaxAllowed(loginDate: Date?, maxInMinutes: Int) : Boolean {
        if(loginDate == null) {
            println("No loginDate found")
            return true
        }
        val diffInMinutes: Long = ((Date().time - loginDate.time) / 1000) / 60

        println("Diff: $diffInMinutes vs Max: $maxInMinutes")

        return diffInMinutes > maxInMinutes
    }
}