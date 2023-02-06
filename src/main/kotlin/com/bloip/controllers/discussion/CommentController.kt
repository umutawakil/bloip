package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.UserEvent
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.services.LoggingService
import com.bloip.services.localization.translation.TranslationService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 6/30/22.
 */
@Controller
class CommentController (
    
    @Autowired val translationService: TranslationService,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
)
{
    @GetMapping("/reply/{discussionId}")
    fun get(
        httpSession: HttpSession,
        request: HttpServletRequest,
        @RequestParam(required = false) c: String?,
        model: Model,
        @PathVariable("discussionId") discussionId: Discussion.DiscussionId

    ): String {
        val start = System.nanoTime()

        model["applicationServerKey"] = applicationProperties.applicationServerKey
        model["baseUrl"]              = applicationProperties.baseUrl
        model["discussionId"]         = discussionId

        val discussion: Discussion = Discussion.get(discussionId = discussionId)!!

        val userId: User.UserId? = WebUtil.getUserIdFromSession(httpSession)
        if (userId != null) {
            model["doublePost"] = discussion.isLastUserToComment(userId)
        }

        val language: Language = httpSession.getAttribute("language") as Language

        println("Language of the controller: ${language.code}")

        model["bodyTranslations"] = translationService.getTranslationMap(context = "recording-states",language)
        model["dialogs"] = translationService.getTranslationMap(context = "recording-states-dialogs",language)

        val eventSequenceId = UUID.randomUUID().toString()
        UserEvent(
            name               = "new_reply_start",
            methodName         = "get",
            context            = "repy",
            durationInNanoSecs = (System.nanoTime() - start) * 1.0,
            url                = "/reply/${discussion.id}",
            userId             = userId,
            sessionId          = httpSession.id,
            sequenceId         = eventSequenceId,
            comment            = "Visitor wants to create a new reply",
            sequenceComplete   = false,
        ).asyncSave()
        model["eventSequenceId"] = eventSequenceId

        return "discussion/reply"
    }

    /**@PostMapping("/reply")
    @ResponseBody
    fun post(
            httpSession : HttpSession,
            request: HttpServletRequest,
            response: HttpServletResponse,
            @RequestParam("discussionId") discussionId: Long
        ) : String {

        val userId: Long = httpSession.getAttribute("userId") as Long
        Discussion.reply(
            userId = userId,
            discussionId = discussionId,
            ipAddress = request.remoteAddr
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussionId)
        loggingService.log("Reply posted: ${discussionURL}")

        return discussionURL
    }**/
}