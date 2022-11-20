package com.bloip.controllers

import com.bloip.domain.localization.CountryDisplayName
import com.bloip.domain.localization.Language
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/5/22.
 */
@ControllerAdvice
class LocalizationAdvice {
    @ModelAttribute("language")
    fun language(httpSession: HttpSession): Language {
        return httpSession.getAttribute("language") as Language
    }
    @ModelAttribute("countryDisplayName")
    fun countryDisplayName(httpSession: HttpSession): CountryDisplayName {
        return httpSession.getAttribute("countryDisplayName") as CountryDisplayName
    }

    @ModelAttribute("headerTranslations")
    fun headerTranslations(httpSession: HttpSession): Map<*, *> {
        return httpSession.getAttribute("headerTranslations") as Map<*, *>
    }

    @ModelAttribute("footerTranslations")
    fun footerTranslations(httpSession: HttpSession): Map<*, *> {
        return httpSession.getAttribute("footerTranslations") as Map<*, *>
    }
}
