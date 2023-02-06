package com.bloip.controllers.inbox

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User

import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Controller
class InboxController (
    @Autowired private val translationService: TranslationService,
    @Autowired private val applicationProperties: ApplicationProperties
) {
    @GetMapping("/inbox")
    fun index(model: Model, httpSession: HttpSession,  @RequestParam(required = false) o: Int?): String {
        val user: User = WebUtil.getUserFromSession(httpSession)!!

        Discussion.showInboxPage(
            userId    = user.id,
            model     = model,
            offset    = o,
            itemsPerPage = applicationProperties.inboxItemsPerPage
        )

        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "inbox",language)

        return "inbox/index"
    }

    @PostMapping("/inbox/subscribe-inbox/{discussionId}")
    @ResponseBody
    fun subscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: DiscussionId): String {
        Discussion.subscribe(
            userId         = WebUtil.getUserIdFromSession(httpSession)!!,
            discussionId   = discussionId
        )
        return "1"
    }

    @PostMapping("/inbox/delete-inbox/{discussionId}")
    @ResponseBody
    fun delete(httpSession: HttpSession, @PathVariable(required = true) discussionId: DiscussionId): String {
        Discussion.leaveConversation(
            userId       = WebUtil.getUserIdFromSession(httpSession)!!,
            discussionId = discussionId
        )
        return "1"
    }

    @GetMapping("/inbox/total")
    @ResponseBody
    fun getInboxTotal(httpSession: HttpSession, httpServletResponse: HttpServletResponse) {
        val user: User? = WebUtil.getUserFromSession(httpSession)
        if (user == null) {
            httpServletResponse.writer.print(0)

        } else {
            Discussion.showInboxTotal(userId = user.id, httpServletResponse = httpServletResponse)
        }
    }
}