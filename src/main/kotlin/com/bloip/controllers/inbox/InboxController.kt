package com.bloip.controllers.inbox

import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.services.DiscussionService
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
    @Autowired val translationService: TranslationService,
    @Autowired val discussionService: DiscussionService
) {
    @GetMapping("/inbox")
    fun index(model: Model, httpSession: HttpSession,  @RequestParam(required = false) o: Long?, @RequestParam(required = false) d: Int?): String {
        val user: User = WebUtil.getUserFromSession(httpSession)!!

        user.showInboxPage(
            model     = model,
            offset    = discussionService.get(discussionId = o),
            direction = d
        )

        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "inbox",language)

        return "inbox/index"
    }

    @PostMapping("/inbox/unsubscribe-inbox/{discussionId}")
    @ResponseBody
    fun unsubscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val user: User = WebUtil.getUserFromSession(httpSession)!!

        discussionService.unsubscribe(
            user         = user,
            discussion   = discussionService.get(discussionId)!!
        )
        return "1"
    }

    @PostMapping("/inbox/subscribe-inbox/{discussionId}")
    @ResponseBody
    fun subscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val user: User = WebUtil.getUserFromSession(httpSession)!!

        discussionService.subscribe(
            user       = user,
            discussion = discussionService.get(discussionId)!!
        )
        return "1"
    }

    @PostMapping("/inbox/delete-inbox/{discussionId}")
    @ResponseBody
    fun delete(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val user: User = WebUtil.getUserFromSession(httpSession)!!

        user.deleteConversation(
            discussion = discussionService.get(discussionId = discussionId)!!
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
            user.showInboxTotal(httpServletResponse)
        }
    }
}