package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.services.LoggingService

import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.AuthUtility
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Controller
class ViewDiscussionController (
    @Autowired val translationService: TranslationService,
    @Autowired val loggingService: LoggingService
){
    @GetMapping("/d/{discussionId}")
    fun get(
        httpSession: HttpSession,
        model: Model,
        @PathVariable("discussionId") discussionId: DiscussionId,
        @RequestParam currentTrack: Int?
    ): String {
        val discussion = Discussion.getForDisplay(discussionId) ?: return "redirect:/unknown/${discussionId}"

        val userId: User.UserId? = WebUtil.getUserIdFromSession(httpSession)
        if (userId != null) {
            Discussion.resetUnreadConversationIndicator(
                userId       = userId,
                discussionId = discussionId
            )
        }

        val language: Language = httpSession.getAttribute("language") as Language

        model["samurai"]      = AuthUtility.isSamurai()
        model["language"]     = language
        model["discussion"]   = discussion
        model["currentTrack"] = currentTrack ?: 1

        model["bodyTranslations"] = translationService.getTranslationMap(
            context = "view-discussion",
            language = language
        )
        model["headerTranslations"] = translationService.getTranslationMap(
            context = "header",
            language = language
        )
        model["footerTranslations"] = translationService.getTranslationMap(
            context = "footer",
            language = language
        )

        return "discussion/view-discussion"
    }

    @GetMapping("/unknown/{discussionId}")
    fun unknownDiscussion(@PathVariable discussionId: DiscussionId, model: Model, httpSession: HttpSession): String {
        loggingService.log("Unknown discussion: ${discussionId}")
        model["discussionId"] = discussionId

        val language: Language = httpSession.getAttribute("language") as Language
        model["dictionary"] = translationService.getTranslationMap(context = "view-discussion",language)

        return "discussion/unknown"
    }
}