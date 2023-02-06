package com.bloip.controllers.discussion

import com.bloip.domain.discussion.Discussion
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@RestController
class GetDiscussionApiController
{
    @GetMapping("/api/d/{discussionId}/{trackNumber}")
    fun get(model: Model,
            @PathVariable("discussionId") discussionId: Discussion.DiscussionId,
            @PathVariable("trackNumber") trackNumber: Int
    ): Any? {
        return Discussion.getCommentsView(
            discussionId = discussionId,
            start        = trackNumber,
            end          = 10
        )
    }
}