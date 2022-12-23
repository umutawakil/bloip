package com.bloip.controllers.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 10/30/22.
 */

@Controller
@Secured("ROLE_SAMURAI")
class AdminController(
    @Autowired private val discussionService: DiscussionService
) {

    @GetMapping("/castelo")
    fun index() : String {
        return "admin/castelo/index"
    }

    @PostMapping("/castelo/extra/title/{discussionId}")
    @ResponseBody
    fun hideTitle(@PathVariable("discussionId") discussionId: Long, res:HttpServletResponse): String {
        val discussion: Discussion = discussionService.get(discussionId = discussionId)!!
        discussion.title = Title("  ")
        discussionService.update(discussion)

        return "Title hidden"
    }

    @PostMapping("/castelo/extra/discussion/{discussionId}")
    @ResponseBody
    fun hideDiscussion(@PathVariable("discussionId") discussionId: Long): String {
        val discussion: Discussion = discussionService.get(discussionId = discussionId)!!
        discussion.censured = true
        discussionService.update(discussion)

        return "Discussion hidden"
    }

    @PostMapping("/castelo/extra/discussion/{discussionId}/{trackNumber}")
    @ResponseBody
    fun hideComment(
        @PathVariable("discussionId") discussionId: Long,
        @PathVariable("trackNumber") trackNumber: Int
    ): String {
        //discussionService.censureComment(discussionId = discussionId, trackNumber = trackNumber)

        return "Comment hidden"
    }

    @PostMapping("/castelo/extra/user/{discussionId}/{trackNumber}")
    @ResponseBody
    fun censureUser(
        @PathVariable("discussionId") discussionId: Long,
        @PathVariable("trackNumber") trackNumber: Int
    ): String {
        //discussionService.censureUser(discussionId = discussionId, trackNumber = trackNumber)

        return "User censured"
    }

    //TODO: block ip address
    /**@PostMapping("/castelo/extra/comment/d/{discussionId}/t/{trackNumber}")
    @ResponseBody
    fun crippleIpAddress(@PathVariable discussionId: Long, @PathVariable commentId: Long): String {

        return "Ip Address crippled"
    }**/
}