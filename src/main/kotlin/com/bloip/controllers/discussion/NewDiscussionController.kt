package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.domain.Topic
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import com.bloip.services.TopicService
import com.bloip.utilities.DiscussionUtility
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (
        @Autowired val discussionService: DiscussionService,
        @Autowired val topicService: TopicService,
        @Autowired val discussionUtility: DiscussionUtility,
        @Autowired val applicationProperties: ApplicationProperties,
        @Autowired val loggingService: LoggingService
    ){

    @GetMapping(value = ["/new-discussion", "/new-discussion/{topicId}"])
    fun get(model: Model, @PathVariable("topicId", required = false) topicId: Long?): String {
        model["maxTitleLength"] = applicationProperties.maxTitleLength
        model["baseURL"]        = applicationProperties.baseURL
        model["topics"]         = topicService.getAll()
        WebUtil.safeSetModelAttribute(model = model, "topicId", topicId)

        return "discussion/create"
    }
    
    @PostMapping("/new-discussion/create")
    @ResponseBody
    fun post(
        request: HttpServletRequest,
        @RequestParam("title") discussionTitle: String,
        @RequestParam("topicId") topicId: Long,
        httpSession : HttpSession
    ) : String {

        val userId: Long = httpSession.getAttribute("userId") as Long
        val topic: Topic = topicService.get(topicId = topicId) ?: throw  NullPointerException("unable to find topic on discussion create")

        val discussion : Discussion = discussionService.create(
            userId    = userId,
            title     = discussionTitle,
            topic     = topic,
            ipAddress = request.remoteAddr
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussion.id)
        loggingService.log("New Discussion created: $discussionURL")

        return discussionURL
    }
}