package com.bloip.controllers

import com.bloip.configuration.ApplicationProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Created by Usman Mutawakil on 9/28/22.
 */

@ControllerAdvice
class BloipAdvice(@Autowired val applicationProperties: ApplicationProperties) {

    @ModelAttribute("baseUrl")
    fun baseUrl(): String {
        return applicationProperties.baseUrl
    }

}