package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
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
 * Created by Usman Mutawakil on 6/30/22.
 */
@Controller
class CommentController (
    @Autowired val translationService: TranslationService,
    @Autowired val applicationProperties: ApplicationProperties
)
{
    @GetMapping("/reply/{discussionId}")
    fun get(
        httpSession: HttpSession,
        request: HttpServletRequest,
        @RequestParam(required = false) c: String?,
        model: Model,
        @PathVariable("discussionId") discussionId: Discussion.DiscussionId

    ): String {
        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["baseUrl"]              = applicationProperties.baseUrl
        model["discussionId"]         = discussionId

        val discussion: Discussion = Discussion.get(discussionId = discussionId)!!

        val userId: User.UserId? = WebUtil.getUserIdFromSession(httpSession)
        if (userId != null) {
            model["doublePost"] = discussion.isLastUserToComment(userId)
        }

        val language: Language = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "recording-states",language)
        model["dialogs"] = translationService.getTranslationMap(context = "recording-states-dialogs",language)

        return "discussion/reply"
    }
}