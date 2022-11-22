package com.bloip.filters

import java.io.IOException
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


/**
 * Created by Usman Mutawakil on 11/21/22.
 */
class CorsFilter : Filter {

    override fun init(filterConfig: FilterConfig?) {
    }

     override fun doFilter(servletRequest: ServletRequest?, servletResponse: ServletResponse, filterChain: FilterChain) {
        val response = servletResponse as HttpServletResponse
        val request = servletRequest as HttpServletRequest?
        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Methods", "*")
        response.setHeader("Access-Control-Allow-Headers", "*")
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Max-Age", "180")
        filterChain.doFilter(servletRequest, servletResponse)
    }

    override fun destroy() {}
}