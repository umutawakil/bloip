package com.bloip.configuration

import com.bloip.filters.SessionFilter
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Component
class FilterRegistrations(
        @Autowired val sessionFilter: SessionFilter,
        @Autowired val loggingService: LoggingService
    ) {

    @PostConstruct
    fun init() {
        loggingService.log("Filter registrations loaded.")
    }

    @Bean
    fun mySessionFilter() : FilterRegistrationBean<SessionFilter> {
        val registrationBean: FilterRegistrationBean<SessionFilter> = FilterRegistrationBean()
        registrationBean.filter = sessionFilter
        registrationBean.addUrlPatterns("/*")
        registrationBean.order = 0
        return registrationBean;
    }
}