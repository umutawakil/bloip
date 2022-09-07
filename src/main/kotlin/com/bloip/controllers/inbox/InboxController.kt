package com.bloip.controllers.inbox

import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseBody
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
    fun index(model: Model, httpSession: HttpSession): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        model["inbox"] = inboxService.getInbox(userId)

        return "inbox/index"
    }

    @PostMapping("/unsubscribe-inbox/{discussionId}")
    @ResponseBody
    fun unsubscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        println("unsubscribe DiscussionID: ${discussionId}, user: ${userId}")

        discussionService.unsubscribe(
            discussionId = discussionId,
            userId       = userId
        )
        return "1"
    }

    @PostMapping("/subscribe-inbox/{discussionId}")
    @ResponseBody
    fun subscribe(httpSession: HttpSession, @PathVariable(required = true) discussionId: Long): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        println("subscribe DiscussionID: ${discussionId}, user: ${userId}")

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

        println("delete DiscussionID: ${discussionId}, user: ${userId}")

        inboxService.deleteConversation(
            discussionId = discussionId,
            userId       = userId
        )

        return "1"
    }

}