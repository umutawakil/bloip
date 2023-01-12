package com.bloip.controllers.admin

import com.bloip.domain.UserEvent
import com.bloip.domain.user.User
import com.bloip.utilities.WebUtil
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 12/18/22.
 */
@Controller
class UserEventController() {

    @ResponseBody
    @PostMapping("/user_event_log")
    fun log(
        @RequestParam(required = true)  name: String,
        @RequestParam(required = true)  methodName: String,
        @RequestParam(required = true)  context: String,
        @RequestParam(required = true)  url: String?,
        @RequestParam(required = true)  sequenceId: String,
        @RequestParam(required = false) comment: String?,
        @RequestParam(required = true)  sequenceComplete: Boolean,
        httpSession: HttpSession
    ): Int {
        UserEvent(
            name             = name,
            methodName       = methodName,
            context          = context,
            url              = url,
            user             = User.findById(userId = WebUtil.getUserIdFromSession(httpSession = httpSession)),
            sessionId        = httpSession.id,
            sequenceId       = sequenceId,
            comment          = comment,
            sequenceComplete = sequenceComplete,
        ).asyncSave()

        return 1
    }
}