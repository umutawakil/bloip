package com.bloip.controllers.discussion

import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Controller
class ViewDiscussionController (@Autowired val discussionService: DiscussionService){
    @GetMapping("/b/{discussionId}")
    fun get(model: Model, @PathVariable("discussionId") discussionId: Int): String {

        val discussion: Discussion? = discussionService.getWithComments(discussionId)
        if (discussion != null) {
            model["discussion"] = discussion
            return "discussion/view-discussion"
        }
        throw RuntimeException("Unknown discussion!")
    }

}