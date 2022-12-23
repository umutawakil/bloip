package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
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
    @Autowired val moderationService: ModerationService
)
{

    /** Below are the methods for overall discussion moderation as oppose to individual comments **/

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
        moderationService.moderateTitle(
            discussionService.get(discussionId)!!
        )
        return "redirect:/castelo/moderation/discussion/${discussionId}?updated=true"
    }

    /** Below is for the individual comments moderation **/

    @GetMapping("/castelo/moderation/discussion/{discussionId}/comment/{trackNumber}")
    fun showEditForComment(
        @PathVariable("discussionId") discussionId: Long,
        @PathVariable("trackNumber") trackNumber: Int,
        @RequestParam("updated", required = false) updated: Boolean?,
        model: Model

    ): String {
        discussionService.displayComment(
            discussion  = discussionService.get(discussionId)!!,
            trackNumber = trackNumber,
            model       = model
        )

        if (updated == true) {
            model["updated"] = updated
        }
        return "admin/moderation/moderate-comment"
    }

    @PostMapping("/castelo/moderation/comment")
    fun moderateComment(
        @RequestParam("discussionId") discussionId: Long,
        @RequestParam("trackNumber") trackNumber: Int
    ): String {
        moderationService.moderateComment(
            discussion  = discussionService.get(discussionId)!!,
            trackNumber = trackNumber
        )
        return "redirect:/castelo/moderation/discussion/$discussionId/comment/$trackNumber?updated=true"
    }

    @PostMapping("/castelo/moderation/user")
    fun moderateUser(
        @RequestParam("discussionId") discussionId: Long,
        @RequestParam("trackNumber") trackNumber: Int
    ): String {
        moderationService.moderateUser(
            discussion = discussionService.get(discussionId)!!,
            trackNumber = trackNumber
        )
        return "redirect:/castelo/moderation/discussion/$discussionId/comment/$trackNumber?updated=true"
    }
}