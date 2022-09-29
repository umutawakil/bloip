package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
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
    @Autowired val inboxService: InboxService
){
    @GetMapping("/d/{discussionId}")
    fun get(httpSession: HttpSession, model: Model, @PathVariable("discussionId") discussionId: Long, @RequestParam currentTrack: Int?): String {

        val discussion: Discussion? = discussionService.get(discussionId)
        if (discussion != null) {
            val userId: Long = httpSession.getAttribute("userId") as Long
            inboxService.resetUnreadConversationIndicator(discussionId = discussionId, userId = userId)

            model["inboxTotal"]   = inboxService.getInboxTotal(userId)
            model["discussion"]   = discussion
            model["currentTrack"] = currentTrack ?: 0
            return "discussion/view-discussion"
        }

        //TODO: Need to decide what possibilities could cause this to happen
        throw RuntimeException("Unknown discussion!")
    }

}