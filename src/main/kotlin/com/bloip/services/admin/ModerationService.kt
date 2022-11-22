package com.bloip.services.admin

import com.bloip.domain.Comment
import com.bloip.domain.User
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Title
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

/**
 * Created by Usman Mutawakil on 11/21/22.
 */
@Service
class ModerationService(
    @Autowired private val adminService: AdminService,
    @Autowired private val discussionService: DiscussionService,
    @Autowired private val commentService: CommentService,
    @Autowired private val userService: UserService
)  {
    fun moderateTitle(discussionId: Long) {
        val discussion: Discussion = discussionService.get(discussionId = discussionId)!!
        var oldTitle = discussion.title
        discussion.title = Title("  ")
        discussionService.updateWithoutBump(discussion)

        adminService.recordEvent("Discussion title removed. ($oldTitle) for discussion: ${discussion.id}")
    }

    fun moderateDiscussion(discussionId: Long) {
        val discussion: Discussion = discussionService.get(discussionId = discussionId)!!
        discussion.censured = true
        discussionService.updateWithoutBump(discussion)

        adminService.recordEvent("Discussion lead recording censured for discussion: ${discussion.id}")
    }

    fun moderateComment(commentId: Long){
        val comment: Comment = commentService.get(id = commentId)!!
        comment.censured = true
        commentService.save(comment)

        adminService.recordEvent(
            "Comment hidden for" +
                    " discussion: ${comment.discussionId}," +
                    " commentId: ${comment.id}," +
                    " Track: ${comment.trackNumber}"
        )
    }

    fun moderateUser(commentId: Long){
        val comment: Comment = commentService.get(id = commentId)!!
        comment.censured = true
        commentService.save(comment)

        val user: User? = userService.findById(comment.userId)
        if(user != null) {
            user.censured = true
            user.censureDate = Date()
            userService.save(user)
            adminService.recordEvent("User ${user.id} has been censured")
        }
    }
}