package com.bloip.controllers.discussion

import com.bloip.configuration.ApplicationProperties
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import javax.transaction.Transactional

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Transactional
@RestController
class GetDiscussionApiController(
        @Autowired val discussionService: DiscussionService,
        private val applicationProperties: ApplicationProperties
    )
{
    @GetMapping("/api/d/{discussionId}/{trackNumber}")
    fun get(model: Model,
            @PathVariable("discussionId") discussionId: Long,
            @PathVariable("trackNumber") trackNumber: Int
    ): Any? {
        return discussionService.getView(
            discussionId = discussionId,
            start        = trackNumber,
            end          = 10
        )
    }
}