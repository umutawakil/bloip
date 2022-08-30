package com.bloip.controllers.inbox

import com.bloip.services.NotificationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Controller
class InboxController (
    @Autowired val notificationService: NotificationService
) {
    @GetMapping("/inbox")
    fun index(model: Model, httpSession: HttpSession): String {
        val userId: Long = httpSession.getAttribute("userId") as Long

        model["inbox"] = notificationService.getInbox(userId)

        return "inbox/index"
    }
}