package com.bloip.controllers.topic

import com.bloip.domain.Discussion
import com.bloip.services.TopicService
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.structures.BumpStack
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import java.util.*
import javax.servlet.http.HttpSession
/**
 * Created by Usman Mutawakil on 9/12/22.
 */
@Controller
class TopicController (
    @Autowired val topicService: TopicService,
    @Autowired val discussionService: DiscussionService,
    @Autowired val inboxService: InboxService
) {
    @GetMapping("/b/{friendlyId}")
    fun index(model: Model,
              @PathVariable(required = true) friendlyId: String,
              @RequestParam(required = false) o: Long?,
              @RequestParam(required = false) d: Int?,
              httpSession: HttpSession): String {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession)

        val page: BumpStack.Page<Long, Discussion> = if( d == null || d >= 0 ) {
            discussionService.getNextPage(
                topicFriendlyId = friendlyId,
                offsetKey = o
            )
        }  else {
            if (o == null) {
                BumpStack.Page(
                    previousOffsetKey = null,
                    nextOffsetKey = null,
                    values = emptyList()
                )
            } else {
                discussionService.getPreviousPage(
                    topicFriendlyId = friendlyId,
                    offsetKey = o
                )
            }
        }

        model["topic"]       = topicService.get(friendlyId = friendlyId)!!
        model["topics"]      = topicService.getAll()
        model["discussions"] = page.values
        model["inboxTotal"]  = inboxService.getInboxTotal(userId)

        WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
        WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)

        return "topic/index"
    }
}