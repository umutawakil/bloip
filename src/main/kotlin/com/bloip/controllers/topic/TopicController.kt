package com.bloip.controllers.topic

import com.bloip.services.TopicService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
@Controller
class TopicController (
    @Autowired val topicService: TopicService
) {
    @GetMapping("/topics")
    fun index(
        model: Model, @RequestParam(required = false) o: Long?,
        @RequestParam(required = false) d: Int?, httpSession: HttpSession
    ): String {
        model["topics"] = topicService.getAll()

        return "topic/index"
    }
}