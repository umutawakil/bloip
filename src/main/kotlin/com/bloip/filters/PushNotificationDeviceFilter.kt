package com.bloip.filters

import org.springframework.stereotype.Component
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.Filter
@Component
class PushNotificationDeviceFilter: Filter {
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain) {
        val req: HttpServletRequest = request as HttpServletRequest
        val res: HttpServletResponse = response as HttpServletResponse

        val deviceKey: String? = req.getParameter("deviceKey")
        val deviceType: String? = req.getParameter("deviceType")
        if(deviceKey != null && deviceType != null) {
            val session = req.getSession(false)
            session.setAttribute("deviceKey", deviceKey)
            session.setAttribute("deviceType", deviceType)
        }
        chain.doFilter(request, response)
    }
}