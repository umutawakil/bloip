package com.bloip.controllers

import com.bloip.domain.localization.Country
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.localization.CountryDisplayName
import com.bloip.domain.localization.Language
import com.bloip.services.localization.translation.LanguageService
import com.bloip.services.localization.translation.TranslationService
import com.bloip.structures.BumpStack
import com.bloip.utilities.AuthUtility
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Controller
class HomeController(
        @Autowired private val languageService: LanguageService,
        @Autowired private val translationService: TranslationService
    )
    {
        @GetMapping("/")
        fun index(
            model: Model,
            @RequestParam(required = false) c: String?,
            @RequestParam(required = false) o: Discussion.DiscussionId?,
            @RequestParam(required = false) d: Int?,
            httpSession: HttpSession,
            request: HttpServletRequest
        ): String {
            val language: Language = httpSession.getAttribute("language") as Language
            val countryDisplayName = httpSession.getAttribute("countryDisplayName") as CountryDisplayName
            val page: BumpStack.Page<Discussion.DiscussionId, Any> = getPage(
                country = countryDisplayName.country,
                d = d,
                o = o
            )

            model["samurai"]              = AuthUtility.isSamurai()
            model["languageDisplayNames"] = languageService.getAllLanguageDisplayNames(language = language)
            model["bodyTranslations"]     = translationService.getTranslationMap(context = "homepage", language)
            model["discussions"]          = page.values

            WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
            WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)

            return "index"
        }

        fun getPage(country: Country, d: Int?, o: Discussion.DiscussionId?) : BumpStack.Page<Discussion.DiscussionId, Any> {
            return if ( d == null || d >= 0 ) {
                Discussion.getNextPage(
                    country = country,
                    offsetKey = o
                )
            }  else {
                if (o == null) {
                    BumpStack.Page(
                        previousOffsetKey = null,
                        nextOffsetKey = null,
                        values = emptyList()
                    )
                } else {
                    Discussion.getPreviousPage(
                        country = country,
                        offsetKey = o
                    )
                }
            }
        }
        @GetMapping("/error-test")
        fun error(
            model: Model, @RequestParam(required = false) o: Long?,
            @RequestParam(required = false) d: Int?, httpSession: HttpSession
        ): String {
            throw RuntimeException("This is an exception")
        }
    }