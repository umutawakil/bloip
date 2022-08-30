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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (
        @Autowired val discussionService: DiscussionService,
        @Autowired val discussionUtility: DiscussionUtility,
        @Autowired val applicationProperties: ApplicationProperties,
        @Autowired val loggingService: LoggingService
    ){

    @GetMapping("/new-discussion")
    fun get(model: Model): String {
        model["baseURL"] = applicationProperties.baseURL
        return "discussion/create"
    }

    @GetMapping("/new-discussion/is-unique")
    @ResponseBody
    fun isUnique(@RequestParam("title") title: String): Int {
        if (discussionService.titleAlreadyExists(title)) {
            return -1;
        } else {
            return 1;
        }
    }

    @PostMapping("/new-discussion/create")
    @ResponseBody
    fun post(
        request: HttpServletRequest,
        @RequestParam("title") discussionTitle: String,
        httpSession : HttpSession
    ) : String {

        val userId: Long = httpSession.getAttribute("userId") as Long

        val discussion : Discussion = discussionService.create(
            userId = userId,
            title = discussionTitle,
            ipAddress = request.remoteAddr
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussion.id)
        loggingService.log("New Discussion created: ${discussionURL}")

        return discussionURL
    }
}