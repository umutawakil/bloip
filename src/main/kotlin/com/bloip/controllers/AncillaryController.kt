package com.bloip.controllers

import com.bloip.domain.localization.Language
import com.bloip.services.localization.translation.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

@Controller
class AncillaryController(
    @Autowired private val translationService: TranslationService
)
{
    @GetMapping("/privacy")
    fun index(
        model: Model,
        httpSession: HttpSession,
        request: HttpServletRequest
    ): String {
        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "ancillary", language)

        return "ancillary/privacy"
    }
}