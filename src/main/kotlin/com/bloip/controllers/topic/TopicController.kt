package com.bloip.controllers.topic

import com.bloip.domain.discussion.Discussion
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
    @GetMapping("/b")
    fun allDiscussionsFromAllTopics(model: Model,
              @RequestParam(required = false) o: Long?,
              @RequestParam(required = false) d: Int?,
              httpSession: HttpSession): String {

        val userId: Long = WebUtil.getUserIdFromSession(httpSession)

        val page: BumpStack.Page<Long, Discussion> = getPage(topicFriendlyId = null, d = d, o = o)

        model["topics"]      = topicService.getAll()
        model["discussions"] = page.values
        model["inboxTotal"]  = inboxService.getInboxTotal(userId)
        WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
        WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)

        return "topic/index"
    }

    @GetMapping("/b/{friendlyId}")
    fun discussionsByGivenTopic(model: Model,
              @PathVariable(required = true) friendlyId: String,
              @RequestParam(required = false) o: Long?,
              @RequestParam(required = false) d: Int?,
              httpSession: HttpSession): String {

        val userId: Long = WebUtil.getUserIdFromSession(httpSession)

        val page: BumpStack.Page<Long, Discussion> = getPage(topicFriendlyId = friendlyId, d = d, o = o)

        model["topics"]      = topicService.getAll()
        model["discussions"] = page.values
        model["inboxTotal"]  = inboxService.getInboxTotal(userId)

        WebUtil.safeSetModelAttribute(model, "topic",  topicService.get(friendlyId = friendlyId))
        WebUtil.safeSetModelAttribute(model,"nextOffsetKey", page.nextOffsetKey)
        WebUtil.safeSetModelAttribute(model,"previousOffsetKey", page.previousOffsetKey)

        return "topic/index"
    }

    fun getPage(topicFriendlyId: String?, d: Int?, o: Long?) : BumpStack.Page<Long, Discussion> {
        return if ( d == null || d >= 0 ) {
            discussionService.getNextPage(
                topicFriendlyId = topicFriendlyId,
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
                    topicFriendlyId = topicFriendlyId,
                    offsetKey = o
                )
            }
        }
    }
}