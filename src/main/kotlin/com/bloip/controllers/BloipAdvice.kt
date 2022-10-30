package com.bloip.controllers

import com.bloip.configuration.EnvironmentConfigs
import com.bloip.msc.Constants
import com.bloip.services.InboxService
import com.bloip.services.UserService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader
import javax.servlet.ServletRequest
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * This class sets global model attributes. Its called before every controller and gives them "advice"
 */
@ControllerAdvice
class BloipAdvice(
    @Autowired val inboxService: InboxService,
    @Autowired val userService: UserService
    ) {

    @ModelAttribute("baseUrl")
    fun baseUrl(): String {
        return EnvironmentConfigs.baseUrl!!
    }

    @ModelAttribute("isMobile")
    fun isMobile(@RequestHeader("user-agent") input: String):Boolean {
        val userAgent = input.lowercase()
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

    @ModelAttribute("isIos")
    fun isIos(@RequestHeader("user-agent") input: String):Boolean {
        val userAgent = input.lowercase()
        val keyWords = setOf(
            "ios",
            "iphone",
            "ipad"
        )
        for(m in keyWords) {
            if (userAgent.contains(m)) {
                return true
            }
        }
        return false
    }

    class AudioInfo(val contentType: String, val fileExtension: String)

    @ModelAttribute("audioType")
    fun audioType(@RequestHeader("user-agent") input: String): AudioInfo {
        return if (isIos(input)) {
            AudioInfo(
                contentType   = Constants.Target_Audio_File_Content_Type,
                fileExtension = Constants.Target_Audio_File_Extension
            )
        } else {
            AudioInfo(
                contentType   = Constants.Temporary_Audio_File_Content_Type,
                fileExtension = Constants.Temporary_Audio_File_Extension
            )
        }
    }

    //TODO: Whats the best way to unit/integration test this?
    @ModelAttribute("inboxTotal")
    fun inboxTotal(httpSession: HttpSession, request: ServletRequest?): Int {
        return inboxService.getInboxTotal(
            userId = WebUtil.getUserIdFromSession(httpSession)
        )
    }

   /* @ModelAttribute("numOfUsersOnline")
    fun numOfUsersOnline(httpSession: HttpSession): Int {
        return userService.numOfUsersOnline()
    }*/
}