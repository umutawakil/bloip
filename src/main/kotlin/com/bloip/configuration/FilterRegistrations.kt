package com.bloip.configuration

import com.bloip.filters.InboxFilter
import com.bloip.filters.LocalizationFilter
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
        @Autowired val localizationFilter: LocalizationFilter,
        @Autowired val inboxFilter: InboxFilter,
        @Autowired val loggingService: LoggingService
    ) {

    @PostConstruct
    fun init() {
        loggingService.log("Filter registrations loaded.\r\n\r\n")
    }

    @Bean
    fun sessionFilterInstanceForRegistration() : FilterRegistrationBean<SessionFilter> {
        val registrationBean: FilterRegistrationBean<SessionFilter> = FilterRegistrationBean()
        registrationBean.filter = sessionFilter
        registrationBean.addUrlPatterns("/*")
        registrationBean.order = 0
        return registrationBean
    }

    @Bean
    fun localizationFilterInstanceForRegistration() : FilterRegistrationBean<LocalizationFilter> {
        val registrationBean: FilterRegistrationBean<LocalizationFilter> = FilterRegistrationBean()
        registrationBean.filter = localizationFilter
        registrationBean.addUrlPatterns("/*")
        registrationBean.order = 1
        return registrationBean
    }

    @Bean
    fun inboxFilterInstanceForRegistration() : FilterRegistrationBean<InboxFilter> {
        val registrationBean: FilterRegistrationBean<InboxFilter> = FilterRegistrationBean()
        registrationBean.filter = inboxFilter
        registrationBean.addUrlPatterns("/*")
        registrationBean.order = 2
        return registrationBean
    }
}