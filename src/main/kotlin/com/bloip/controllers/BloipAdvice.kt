package com.bloip.controllers

import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.EnvironmentConfigs
import com.bloip.msc.Constants

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader

/**
 * This class sets global model attributes. Its called before every controller and gives them "advice"
ControllerAdvice */
@ControllerAdvice
class BloipAdvice(
    @Autowired val applicationProperties: ApplicationProperties
    ) {

    @ModelAttribute("ENABLE_REMOTE_SERVICES")
    fun remoteServices(): String {
        return applicationProperties.enableRemoteServices
    }

    @ModelAttribute("mscCdn")
    fun mscCdn(): String {
        /*if(applicationProperties.baseUrl.contains("localhost")) {
            return applicationProperties.baseUrl
        }*/
        return applicationProperties.mscCdn + "/" + viewVersion()
    }

    @ModelAttribute("viewVersion")
    fun viewVersion(): String {
        return applicationProperties.viewVersion
    }

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
}