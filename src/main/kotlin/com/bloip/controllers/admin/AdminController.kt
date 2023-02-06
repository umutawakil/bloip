package com.bloip.controllers.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.discussion.value.Title
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional

/**
 * Created by Usman Mutawakil on 10/30/22.
 */
@Controller
@Secured("ROLE_SAMURAI")
class AdminController(
    
) {

    @GetMapping("/castelo")
    fun index() : String {
        return "admin/castelo/index"
    }

    @PostMapping("/castelo/extra/title/{discussionId}")
    @ResponseBody
    fun hideTitle(@PathVariable("discussionId") discussionId: DiscussionId, res:HttpServletResponse): String {
        Discussion.censureTitle(
            discussion = Discussion.get(discussionId)!!
        )

        return "Title hidden"
    }

    @PostMapping("/castelo/extra/discussion/{discussionId}")
    @ResponseBody
    fun hideDiscussion(@PathVariable("discussionId") discussionId: DiscussionId): String {

        //TODO: What is suppose to happen here?
        /*val discussion: Discussion = Discussion.get(discussionId = discussionId)!!
        discussion.censured = true
        Discussion.update(discussion)*/

        return "Discussion hidden"
    }

    @PostMapping("/castelo/extra/discussion/{discussionId}/{trackNumber}")
    @ResponseBody
    fun hideComment(
        @PathVariable("discussionId") discussionId: DiscussionId,
        @PathVariable("trackNumber") trackNumber: Int
    ): String {
        //Discussion.censureComment(discussionId = discussionId, trackNumber = trackNumber)

        return "Comment hidden"
    }

    @PostMapping("/castelo/extra/user/{discussionId}/{trackNumber}")
    @ResponseBody
    fun censureUser(
        @PathVariable("discussionId") discussionId: DiscussionId,
        @PathVariable("trackNumber") trackNumber: Int
    ): String {
        //Discussion.censureUser(discussionId = discussionId, trackNumber = trackNumber)

        return "User censured"
    }

    //TODO: block ip address
    /**@PostMapping("/castelo/extra/comment/d/{discussionId}/t/{trackNumber}")
    @ResponseBody
    fun crippleIpAddress(@PathVariable discussionId: Long, @PathVariable commentId: Long): String {

        return "Ip Address crippled"
    }**/
}