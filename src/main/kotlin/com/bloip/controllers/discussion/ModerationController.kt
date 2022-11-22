package com.bloip.controllers.discussion

import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.admin.ModerationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Created by Usman Mutawakil on 11/21/22.
 */
@Controller
class ModerationController(
    @Autowired val discussionService: DiscussionService,
    @Autowired val commentService: CommentService,
    @Autowired val moderationService: ModerationService
)
{
    @GetMapping("/castelo/moderation/discussion/{discussionId}")
    fun showEditForDiscussionHomeView(
        @PathVariable("discussionId") discussionId: Long,
        @RequestParam("updated", required = false) updated: Boolean?,
        model: Model
    ): String {
        val discussion: Discussion = discussionService.get(discussionId)!!

        model["discussion"] = discussion
        if (updated == true) {
            model["updated"] = updated
        }
        return "admin/moderation/moderate-discussion"
    }

    @PostMapping("/castelo/moderation/title")
    fun moderateTitle(@RequestParam("discussionId", required = true) discussionId: Long,
                      model: Model
    ): String {
        moderationService.moderateTitle(discussionId)
        return "redirect:/castelo/moderation/discussion/${discussionId}?updated=true"
    }

    @PostMapping("/castelo/moderation/discussion")
    fun moderateLeadRecording(
        @RequestParam("discussionId") discussionId: Long,
        model: Model
    ): String {
        moderationService.moderateDiscussion(discussionId)
        return "redirect:/castelo/moderation/discussion/${discussionId}?updated=true"
    }

    //TODO: Bug: Doesn't like showModerateComment as a method name
    //@GetMapping("/test-test/")
    @GetMapping("/castelo/moderation/comment/{commentId}")
    fun showEditForComment(
        @PathVariable("commentId") commentId: Long,
        @RequestParam("updated", required = false) updated: Boolean?,
        model: Model

    ): String {
        val comment: Comment = commentService.get(commentId)!!
        model["comment"] = comment
        if (updated == true) {
            model["updated"] = updated
        }
        return "admin/moderation/moderate-comment"
    }

    @PostMapping("/castelo/moderation/comment")
    fun moderateComment(
        @RequestParam("commentId") commentId: Long
    ): String {
        moderationService.moderateComment(commentId = commentId )
        return "redirect:/castelo/moderation/comment/${commentId}?updated=true"
    }

    @PostMapping("/castelo/moderation/user")
    fun moderateUser(
        @RequestParam("commentId") commentId: Long
    ): String {
        moderationService.moderateUser(commentId = commentId )
        return "redirect:/castelo/moderation/comment/${commentId}?updated=true"
    }
}