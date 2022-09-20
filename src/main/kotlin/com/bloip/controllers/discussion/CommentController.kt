package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import com.bloip.utilities.DiscussionUtility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/30/22.
 */

@Controller
class CommentController (
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionUtility: DiscussionUtility,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
)
{
    @GetMapping("/reply/{discussionId}")
    fun get(model: Model, @PathVariable("discussionId") discussionId: Long): String {
        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["baseURL"]              = applicationProperties.baseURL
        model["discussionId"]         = discussionId
        return "discussion/reply"
    }

    @PostMapping("/reply")
    @ResponseBody
    fun post(
            httpSession : HttpSession,
            request: HttpServletRequest,
            response: HttpServletResponse,
            @RequestParam("discussionId") discussionId: Long
        ) : String {

        val userId: Long = httpSession.getAttribute("userId") as Long
        discussionService.reply(
            userId = userId,
            discussionId = discussionId,
            ipAddress = request.remoteAddr
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussionId)
        loggingService.log("Reply posted: ${discussionURL}")

        return discussionURL
    }
}