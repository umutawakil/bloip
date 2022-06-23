package com.bloip.controllers.discussion

import com.bloip.services.DiscussionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Created by Usman Mutawakil on 6/23/22.
 */
@Controller
class NewDiscussionController (@Autowired val discussionService: DiscussionService){
    @GetMapping("/new-discussion")
    fun get(): String? {
        return "discussion/new-discussion"
    }
}