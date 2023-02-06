package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User

import com.bloip.services.localization.translation.LanguageService
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
    @Autowired val languageService: LanguageService
){
    @GetMapping("/d/{discussionId}/l/{languageCode}")
    fun get(
        httpSession: HttpSession,
        model: Model,
        @PathVariable("discussionId") discussionId: DiscussionId,
        @PathVariable("languageCode") languageCode: String?,
        @RequestParam currentTrack: Int?
    ): String {

        //TODO: When discussion is null a 404 should be returned and some logging as it may mean a stale reference or a need to refresh
        // from the DB
        val discussion = Discussion.getForDisplay(discussionId) ?: throw RuntimeException("Unknown discussion!")

        val userId: User.UserId? = WebUtil.getUserIdFromSession(httpSession)
        if (userId != null) {
            Discussion.resetUnreadConversationIndicator(
                userId       = userId,
                discussionId = discussionId
            )
        }

        val language: Language = if (languageCode != null) {
            languageService.getCanonicalByCode(code = languageCode)!!
        } else {
            httpSession.getAttribute("language") as Language
        }

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
}