package com.bloip.controllers

import com.bloip.configuration.EnvironmentConfigs
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Created by Usman Mutawakil on 9/28/22.
 */

@ControllerAdvice
class BloipAdvice {

    @ModelAttribute("baseUrl")
    fun baseUrl(): String {
        return EnvironmentConfigs.baseUrl
    }
}