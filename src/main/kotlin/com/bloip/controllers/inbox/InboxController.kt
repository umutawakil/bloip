package com.bloip.controllers.inbox

import com.bloip.domain.inbox.InboxItem
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Controller
class InboxController (
    @Autowired val inboxService: InboxService,
    @Autowired val discussionService: DiscussionService
) {
    @GetMapping("/inbox")
    fun index(model: Model, httpSession: HttpSession,  @RequestParam(required = false) o: Long?, @RequestParam(required = false) d: Int?): String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession)
        val page: BumpStack.Page<Long, InboxItem> = if( d == null || d >= 0 ) {
            inboxService.getNextPage(userId = userId, offsetKey = o)
        }  else {
            if (o == null) {
                BumpStack.Page(previousOffsetKey = null, nextOffsetKey = null, values = emptyList())
            } else {
                inboxService.getPreviousPage(userId = userId, offsetKey = o)
            }
        }

        WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
        WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)
        model["inbox"]        = page.values

        return "inbox/index"
    }

    @PostMapping("/unsubscribe-inbox/{discussionId}")
    @ResponseBody
    fun unsubscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession)

        discussionService.unsubscribe(
            discussionId = discussionId,
            userId       = userId
        )
        return "1"
    }

    @PostMapping("/subscribe-inbox/{discussionId}")
    @ResponseBody
    fun subscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession)

        discussionService.subscribe(
            discussionId = discussionId,
            userId       = userId
        )
        return "1"
    }

    @PostMapping("/delete-inbox/{discussionId}")
    @ResponseBody
    fun delete(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        inboxService.deleteConversation(
            discussionId = discussionId,
            userId       = userId
        )

        return "1"
    }

    @GetMapping("/inbox-total")
    @ResponseBody
    fun getInboxTotal(httpSession: HttpSession): Int {
        val userId: Long = httpSession.getAttribute("userId") as Long

        return inboxService.getInboxTotal(userId = userId)
    }
}