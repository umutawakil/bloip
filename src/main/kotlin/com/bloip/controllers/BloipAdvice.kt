package com.bloip.controllers

import com.bloip.configuration.EnvironmentConfigs
import com.bloip.services.InboxService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader
import javax.servlet.http.HttpSession

/**
 * This class sets global model attributes. Its called before every controller and gives them "advice"
 */
@ControllerAdvice
class BloipAdvice(@Autowired val inboxService: InboxService) {

    @ModelAttribute("baseUrl")
    fun baseUrl(): String {
        return EnvironmentConfigs.baseUrl
    }

    @ModelAttribute("isMobile")
    fun isMobile(@RequestHeader("user-agent") userAgent: String):Boolean {
        val mobileKeyWords = setOf(
            "mobile",
            "tablet",
            "ios",
            "iphone",
            "ipad",
            "tablet",
            "android"
        )
        for(m in mobileKeyWords) {
            if (userAgent.contains(m)) {
                return true
            }
        }
        return false
    }

    @ModelAttribute("inboxTotal")
    fun inboxTotal(httpSession: HttpSession): Int {
        return inboxService.getInboxTotal(userId = WebUtil.getUserIdFromSession(httpSession))
    }
}