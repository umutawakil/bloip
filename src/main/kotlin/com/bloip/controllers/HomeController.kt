package com.bloip.controllers

import com.bloip.services.TopicService
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Controller
class HomeController(
        @Autowired val discussionService: DiscussionService,
        @Autowired val inboxService: InboxService,
        @Autowired val topicService: TopicService
    )
    {
        @GetMapping("/")
        fun index(
            model: Model, @RequestParam(required = false) o: Long?,
            @RequestParam(required = false) d: Int?, httpSession: HttpSession
        ): String {
            val userId: Long     = WebUtil.getUserIdFromSession(httpSession)
            model["topics"]      = topicService.getAll()
            model["inboxTotal"]  = inboxService.getInboxTotal(userId)

            return "index"
        }

        @GetMapping("/error-test")
        fun error(
            model: Model, @RequestParam(required = false) o: Long?,
            @RequestParam(required = false) d: Int?, httpSession: HttpSession
        ): String {
            throw RuntimeException("This is an exception")
        }
}