package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.User
import com.bloip.domain.discussion.Title
import com.bloip.domain.localization.Language
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import com.bloip.services.UserService
import com.bloip.services.localization.translation.TranslationService
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
        @Autowired val translationService: TranslationService,
        @Autowired val userService: UserService,
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
        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["maxTitleLength"]       = Title.MAX_TITLE_LENGTH
        model["baseUrl"]              = applicationProperties.baseUrl

        val userId: Long? = WebUtil.getUserIdFromSession(httpSession = httpSession)
        if (userId != null) {
            val user: User = userService.findById(userId = userId)!!
            model["discussionCreationLimitReached"] = userService.isDiscussionCreationLimitReached(user)
            println("Creation Limit Reached: " + userService.isDiscussionCreationLimitReached(user))
        } else {
            println("New visitor with no user account")
        }
        val language: Language    = httpSession.getAttribute("language") as Language
        model["bodyTranslations"] = translationService.getTranslationMap(context = "recording-states",language)
        model["dialogs"]          = translationService.getTranslationMap(context = "recording-states-dialogs",language)

        WebUtil.safeSetModelAttribute(model = model, "topicId", topicId)

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