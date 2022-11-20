package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.localization.Language
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.services.localization.translation.LanguageService
import com.bloip.services.localization.translation.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Controller
class ViewDiscussionController (
    @Autowired val discussionService: DiscussionService,
    @Autowired val inboxService: InboxService,
    @Autowired val translationService: TranslationService,
    @Autowired val languageService: LanguageService
){
    @GetMapping("/d/{discussionId}/l/{languageCode}")
    fun get(
        httpSession: HttpSession,
        model: Model,
        @PathVariable("discussionId") discussionId: Long,
        @PathVariable("languageCode") languageCode: String?,
        @RequestParam currentTrack: Int?
    ): String {

        //TODO: When discussion is null a 404 should be returned and some logging as it may mean a stale reference or a need to refresh
        // from the DB
        val discussion: Discussion = discussionService.get(discussionId) ?: throw RuntimeException("Unknown discussion!")

        /** Clear the inbox record if a user is viewing a discussion they are subscribed to **/
        val userId: Long? = httpSession.getAttribute("userId") as Long?
        if(userId != null) {
            inboxService.resetUnreadConversationIndicator(discussionId = discussionId, userId = userId)
        }

        val language: Language = if(languageCode != null) { languageService.getCanonicalByCode(code = languageCode)!! }
        else {
            httpSession.getAttribute("language") as Language
        }

        model["language"]         = language
        model["discussion"]       = discussion
        model["currentTrack"]     = currentTrack ?: 0

        model["bodyTranslations"]   = translationService.getTranslationMap(
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