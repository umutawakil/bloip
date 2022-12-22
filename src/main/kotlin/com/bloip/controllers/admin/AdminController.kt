package com.bloip.controllers.admin

import com.bloip.domain.Comment
import com.bloip.domain.user.User
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 10/30/22.
 */

@Controller
@Secured("ROLE_SAMURAI")
class AdminController(
    @Autowired private val discussionService: DiscussionService,
    @Autowired private val commentService: CommentService,
    @Autowired private val userService: UserService
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

    @GetMapping("/castelo/extra/title/{discussionId}")
    @ResponseBody
    fun getTitle() : String {
        return "testing"
    }

    @PostMapping("/castelo/extra/discussion/{discussionId}")
    @ResponseBody
    fun hideDiscussion(@PathVariable("discussionId") discussionId: Long): String {
        val discussion: Discussion = discussionService.get(discussionId = discussionId)!!
        discussion.censured = true
        discussionService.update(discussion)

        return "Discussion hidden"
    }

    @PostMapping("/castelo/extra/comment/{commentId}")
    @ResponseBody
    fun hideComment(@PathVariable("commentId") commentId: Long): String {
        val comment: Comment = commentService.get(id = commentId)!!
        comment.censured = true
        commentService.save(comment)

        return "Comment hidden"
    }

    @PostMapping("/castelo/extra/user/{commentId}")
    @ResponseBody
    fun censureUser(@PathVariable("commentId") commentId: Long): String {
        val comment: Comment = commentService.get(id = commentId)!!
        comment.censured = true
        commentService.save(comment)

        val user: User? = userService.findById(comment.userId)
        if(user != null) {
            user.censured = true
            user.censureDate = Date()
            userService.save(user)
        }

        return "User censured"
    }

    //TODO: block ip address
    /**@PostMapping("/castelo/extra/comment/d/{discussionId}/t/{trackNumber}")
    @ResponseBody
    fun crippleIpAddress(@PathVariable discussionId: Long, @PathVariable commentId: Long): String {

        return "Ip Address crippled"
    }**/
}