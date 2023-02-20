package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.user.User
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Language

import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (
        @Autowired val translationService: TranslationService,
        @Autowired val applicationProperties: ApplicationProperties
    ){
    @GetMapping(value = ["/new-discussion", "/new-discussion/{topicId}"])
    fun get(
        httpSession: HttpSession,
        request: HttpServletRequest,
        @RequestParam(required = false) c: String?,
        model: Model,
        @PathVariable("topicId", required = false) topicId: Long?

    ): String {
        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["maxTitleLength"]       = Title.MAX_TITLE_LENGTH
        model["baseUrl"]              = applicationProperties.baseUrl

        val user: User? = WebUtil.getUserFromSession(httpSession)
        user?.showIfDiscussionCreationLimitReached(model)

        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "recording-states",language)
        model["dialogs"]          = translationService.getTranslationMap(context = "recording-states-dialogs",language)

        WebUtil.safeSetModelAttribute(model = model, "topicId", topicId)

        return "discussion/create"
    }
}