package com.bloip.controllers.inbox

import com.bloip.domain.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Controller
class InboxController (
    @Autowired val inboxService: InboxService
) {
    @GetMapping("/inbox")
    fun index(model: Model, httpSession: HttpSession): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        model["inbox"] = inboxService.getInbox(userId)

        return "inbox/index"
    }
}