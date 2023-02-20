package com.bloip.controllers.admin

import com.bloip.domain.BrowserEvent
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

/**
 * TODO: Is it efficient to have this run on a single executor service? Should this be moved to DynamoDB or some other
 * low priority event queueing service?
 */
@Controller
class UserEventController() {
    @ResponseBody
    @PostMapping("/user_event_log")
    fun log(
        @RequestParam(required = true)  name: String,
        @RequestParam(required = true)  value: String,
        @RequestParam(name = "browser-info", required = true)  browserInfo: String
    ): Int {
        BrowserEvent(
            name        = name,
            value       = value,
            browserInfo = browserInfo
        ).asyncSave()

        return 1
    }
}