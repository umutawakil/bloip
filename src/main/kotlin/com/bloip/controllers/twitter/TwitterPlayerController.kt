package com.bloip.controllers.twitter

import com.bloip.domain.discussion.Discussion
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
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
class TwitterPlayerController(
    @Autowired val discussionService: DiscussionService
)
{
    @GetMapping("/twitter-player/{discussionId}")
    fun get(
        @PathVariable("discussionId") discussionId: Long,
        model: Model,response: HttpServletResponse
    ): String {

        val discussion: Discussion? = discussionService.get(discussionId = discussionId)
        if(discussion == null) {
            response.status = 404
            response.sendError(404, "Discussion not found")
            return "error/404"
        }
        model["audioUrl"] = discussion.audioUrl

        return "twitter/player"

    }
}