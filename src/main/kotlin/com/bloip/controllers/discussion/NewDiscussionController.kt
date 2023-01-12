package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.user.User
import com.bloip.domain.UserEvent
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Language
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (
        @Autowired val discussionService: DiscussionService,
        @Autowired val translationService: TranslationService,
        @Autowired val applicationProperties: ApplicationProperties,
        @Autowired val loggingService: LoggingService
    ){

    @GetMapping(value = ["/new-discussion", "/new-discussion/{topicId}"])
    fun get(
        httpSession: HttpSession,
        request: HttpServletRequest,
        @RequestParam(required = false) c: String?,
        model: Model,
        @PathVariable("topicId", required = false) topicId: Long?

    ): String {
        val start = System.nanoTime()
        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["maxTitleLength"]       = Title.MAX_TITLE_LENGTH
        model["baseUrl"]              = applicationProperties.baseUrl

        val user: User? = WebUtil.getUserFromSession(httpSession)
        if (user != null) {
            println("User: ${user.id}, DiscussionCount: ${user.isDiscussionCreationLimitReached()}")
            model["discussionCreationLimitReached"] = user.isDiscussionCreationLimitReached()
            println("Creation Limit Reached: " + user.isDiscussionCreationLimitReached())
        } else {
            println("New visitor with no user account")
        }
        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "recording-states",language)
        model["dialogs"]          = translationService.getTranslationMap(context = "recording-states-dialogs",language)

        WebUtil.safeSetModelAttribute(model = model, "topicId", topicId)

        val eventSequenceId      = UUID.randomUUID().toString()
        model["eventSequenceId"] = eventSequenceId

        UserEvent(
            name               = "new_discussion_start",
            methodName         = "get",
            context            = "discussion_creation",
            durationInNanoSecs = (System.nanoTime() - start) * 0.0,
            url                = "/new-discussion",
            user               = User.findById(userId = WebUtil.getUserIdFromSession(httpSession = httpSession)),
            sessionId          = httpSession.id,
            sequenceId         = eventSequenceId,
            comment            = "Visitor want to create a new discussion",
            sequenceComplete   = false,
        ).saveNow()

        return "discussion/create"
    }
    
    /*@PostMapping("/new-discussion/create")
    @ResponseBody
    fun post(
        request: HttpServletRequest,
        @RequestParam("title") discussionTitle: Title,
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
    }*/
}