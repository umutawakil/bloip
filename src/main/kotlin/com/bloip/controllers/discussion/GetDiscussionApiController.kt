package com.bloip.controllers.discussion

import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@RestController
class GetDiscussionApiController(@Autowired val discussionService: DiscussionService) {
    @GetMapping("/api/d/{discussionId}")
    fun get(model: Model, @PathVariable("discussionId") discussionId: Long): Discussion? {
        return discussionService.get(discussionId)
    }
}