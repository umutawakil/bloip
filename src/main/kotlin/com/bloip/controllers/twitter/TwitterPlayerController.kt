package com.bloip.controllers.twitter

import com.bloip.domain.discussion.Discussion

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import javax.servlet.http.HttpServletResponse

/**
 * Created by Usman Mutawakil on 11/12/22.
 */
@Controller
class TwitterPlayerController
{
    @GetMapping("/twitter-player/{discussionId}")
    fun get(
        @PathVariable("discussionId") discussionId: Discussion.DiscussionId,
        model: Model,response: HttpServletResponse
    ): String {
        val discussion = Discussion.getDtoFromFirstCommentForDisplay(discussionId = discussionId)
        if(discussion == null) {
            response.status = 404
            response.sendError(404, "Discussion not found")
            return "error/404"
        }
        model["discussion"] = discussion

        return "twitter/player"

    }
}