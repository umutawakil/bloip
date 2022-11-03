package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.controllers.BloipAdvice
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Title
import com.bloip.domain.discussion.YoutubeLink
import com.bloip.services.CountryService
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import com.bloip.services.cdn.CdnInfo
import com.bloip.services.cdn.CdnUploadService
import com.bloip.utilities.DiscussionUtility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/27/22.
 */
@Controller
class UploadController(
    @Autowired val cdnUploadService: CdnUploadService,
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionUtility: DiscussionUtility,
    @Autowired val countryService: CountryService,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
) {
    @GetMapping("/cdn-info")
    @ResponseBody
    fun cdnInfo(httpSession : HttpSession, @ModelAttribute("audioType") audioInfo: BloipAdvice.AudioInfo): CdnInfo {
        println("AudioInfo.contentType: ${audioInfo.contentType}, AudioInfo.fileExtension: ${audioInfo.fileExtension}")
        val cdnInfo: CdnInfo = cdnUploadService.getInfo(
            userId     = httpSession.getAttribute("userId") as Long,
            audioInfo  = audioInfo
        )
        httpSession.setAttribute("cdninfo", cdnInfo) //Hooray for state!
        return cdnInfo
    }

    @PostMapping("/discussion-audio-uploaded")
    @ResponseBody
    fun initialDiscussionUploadComplete(
        request: HttpServletRequest,
        @RequestParam("title") discussionTitle: Title,
        @RequestParam("duration") duration: Int,
        @RequestParam("youtubeLink") youtubeLink: YoutubeLink?,
        @RequestParam("countryCode") countryCode: String,
        httpSession : HttpSession
    ): String {
        loggingService.log("Discussion: File successfully uploaded")
        val userId: Long = httpSession.getAttribute("userId") as Long
        val cdnInfo: CdnInfo = httpSession.getAttribute("cdninfo") as CdnInfo

        //val countryCode = request.getHeader("CloudFront-Viewer-Country") ?: "us"

        val discussion : Discussion = discussionService.create(
            userId      = userId,
            title       = discussionTitle,
            ipAddress   = request.remoteAddr,
            duration    = duration,
            fileName    = cdnInfo.fileName,
            youtubeLink = youtubeLink,
            country     = countryService.getByCode(countryCode)!!
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussion.id)
        loggingService.log("New Discussion created: $discussionURL")

        return discussionURL
    }

    @PostMapping("/reply-audio-uploaded")
    @ResponseBody
    fun replyUploadComplete(
            httpSession : HttpSession,
            request: HttpServletRequest,
            @RequestParam("discussionId") discussionId: Long,
            @RequestParam("duration") duration: Int
    ): String {
        loggingService.log("Reply: File successfully uploaded")

        val userId: Long = httpSession.getAttribute("userId") as Long
        val cdnInfo: CdnInfo = httpSession.getAttribute("cdninfo") as CdnInfo

        discussionService.reply(
            userId       = userId,
            discussionId = discussionId,
            ipAddress    = request.remoteAddr,
            duration     = duration,
            fileName     = cdnInfo.fileName
        )

        val discussionURL: String = discussionUtility.getDiscussionUrlFromId(discussionId)
        loggingService.log("Reply posted: ${discussionURL}")

        return discussionURL
    }

    @GetMapping("/upload-complete")
    @ResponseBody
    fun uploadComplete(): Int {
        loggingService.log("File successfully uploaded to cdn")
        /** TODO: At the moment this is assumed to be amazon but needs to be fixed so we check the ip-address etc. This is what
        should be creating the discussion or reply but for ease of testing we swallow the front-end suchess in an aysnc ajax call so that
         the app can be fully tested locally without a deploy**/

        //TODO: Ensure upload-complete is hit without the need for a sessionfilter

        return 1
    }
}