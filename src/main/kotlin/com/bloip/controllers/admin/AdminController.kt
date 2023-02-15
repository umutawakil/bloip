package com.bloip.controllers.admin

import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*

/**
 * Created by Usman Mutawakil on 10/30/22.
 */
@Controller
@Secured("ROLE_SAMURAI")
class AdminController {
    @GetMapping("/castelo")
    fun index(
        @RequestParam(required=false) success: Int?,
        @RequestParam(required=false) m:String?,
        model: Model
    ): String {
        if (success != null) {
            model["success"] = success
        }
        if (m != null) {
            model["m"] = m
        }
        return "admin/moderation/index"
    }

    @PostMapping("/castelo/delete-discussion")
    fun deleteDiscussion(
        @RequestParam discussionId: DiscussionId
    ): String {
        Discussion.delete(
            discussionId = discussionId
        )
        return "redirect:/castelo?success=1&m=discussion"
    }

    @PostMapping("/castelo/censor-comment")
    fun censorComment(
        @RequestParam discussionId: DiscussionId,
        @RequestParam trackNumber: Int
    ): String {
        Discussion.censorComment(
            discussionId = discussionId,
            trackNumber  = trackNumber
        )
        return "redirect:/castelo?success=1&m=comment"
    }

    @PostMapping("/castelo/censor-user")
    fun censorUser(
        @RequestParam discussionId: DiscussionId,
        @RequestParam trackNumber: Int
    ): String {
        Discussion.censorUser(
            discussionId = discussionId,
            trackNumber  = trackNumber
        )
        return "redirect:/castelo?success=1&m=user"
    }
}