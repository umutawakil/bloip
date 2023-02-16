package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.controllers.BloipAdvice
import com.bloip.domain.user.User
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.CountryDisplayName
import com.bloip.domain.localization.Language

import com.bloip.services.LoggingService
import com.bloip.services.cdn.CdnInfo
import com.bloip.services.cdn.CdnUploadService
import com.bloip.utilities.DiscussionUtility
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/27/22.
 */
@Controller
class UploadController(
    @Autowired val cdnUploadService: CdnUploadService,
    @Autowired val discussionUtility: DiscussionUtility,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
) {
    companion object {
        const val UPLOAD_COMPLETE_URL = "/upload-complete"
    }

    @PostMapping("/cdn-info")
    @ResponseBody
    fun cdnInfo(httpSession : HttpSession, @ModelAttribute("audioType") audioInfo: BloipAdvice.AudioInfo): CdnInfo {
        val userId: User.UserId = httpSession.getAttribute("userId") as User.UserId
        val user: User = User.findById(userId)!!

        val cdnInfo =  user.doIfCensored(
            deny = {
                        loggingService.log("Censured user: $userId attempting to post.")
                        CdnInfo(
                            censored = true,
                            uuid = "",
                            policy = "",
                            signature = "",
                            fileName= "",
                            audioCdnUploadUrl= "",
                            date= "",
                            credential= "",
                            redirectUrl= ""
                        )
            },
            allow = {
                        val cdnInfo: CdnInfo = cdnUploadService.getInfo(
                            userId = userId,
                            audioInfo = audioInfo
                        )
                        httpSession.setAttribute("cdninfo", cdnInfo) //Hooray for state!
                        cdnInfo
            }
        )
        return cdnInfo
    }

    @PostMapping("/discussion-audio-uploaded")
    @ResponseBody
    fun initialDiscussionUploadComplete(
        request: HttpServletRequest,
        @RequestParam("title") discussionTitle: Title,
        @RequestParam("duration") duration: Int,
        @RequestParam("eventSequenceId") eventSequenceId: String,
        httpSession : HttpSession
    ): String {
        loggingService.log("Discussion: File successfully uploaded")
        val userId: User.UserId                    = WebUtil.getUserIdFromSession(httpSession = httpSession)!!
        val cdnInfo: CdnInfo                       = httpSession.getAttribute("cdninfo") as CdnInfo
        val countryDisplayName: CountryDisplayName = httpSession.getAttribute("countryDisplayName") as  CountryDisplayName

        val discussion : Discussion = Discussion.create(
            userId          = userId,
            title           = discussionTitle,
            duration        = duration,
            fileName        = cdnInfo.fileName,
            country         = countryDisplayName.country,
            eventSequenceId = eventSequenceId,
            language        = httpSession.getAttribute("language") as Language
        )
        httpSession.removeAttribute("cdninfo")

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussion.id)
        loggingService.log("New Discussion created: $discussionURL")

        return discussionURL
    }

    @PostMapping("/reply-audio-uploaded")
    @ResponseBody
    fun replyUploadComplete(
            httpSession : HttpSession,
            request: HttpServletRequest,
            @RequestParam("discussionId") discussionId: Discussion.DiscussionId,
            @RequestParam("duration") duration: Int,
            @RequestParam("eventSequenceId") eventSequenceId: String,
    ): String {
        loggingService.log("Reply: File successfully uploaded")

        val userId: User.UserId = WebUtil.getUserIdFromSession(httpSession)!!
        val cdnInfo: CdnInfo = httpSession.getAttribute("cdninfo") as CdnInfo

        Discussion.reply(
            userId          = userId,
            discussionId    = discussionId,
            duration        = duration,
            fileName        = cdnInfo.fileName,
            eventSequenceId = eventSequenceId,
            language        = httpSession.getAttribute("language") as Language
        )
        httpSession.removeAttribute("cdninfo")

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussionId)
        loggingService.log("Reply posted: $discussionURL")

        return discussionURL
    }

    @RequestMapping(UPLOAD_COMPLETE_URL)
    @ResponseBody
    fun uploadComplete(req: HttpServletRequest, res: HttpServletResponse): Int {
        loggingService.log("File successfully uploaded to cdn")
        /** TODO: At the moment this is assumed to be amazon but needs to be fixed so we check the ip-address etc. This is what
        should be creating the discussion or reply but for ease of testing we swallow the front-end suchess in an aysnc ajax call so that
         the app can be fully tested locally without a deploy**/

        loggingService.log("Upload complete: " + res.getHeader("Origin"))
        //val allowedOrigins: List<String> = listOf("https://dev.bloip.com","https://bloip.com")
        res.addHeader("Access-Control-Allow-Origin", "*")

        return 1
    }
}