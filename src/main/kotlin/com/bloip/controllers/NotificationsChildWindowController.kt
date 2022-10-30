package com.bloip.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 10/28/22.
 */

@Controller
class NotificationsChildWindowController
{
    @GetMapping("/child-notifications-window")
    fun get(httpSession: HttpSession): String {
        return "child-notifications-window.html"
    }
}