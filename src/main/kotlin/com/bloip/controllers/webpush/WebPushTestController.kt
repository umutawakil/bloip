package com.bloip.controllers.webpush

import com.bloip.utilities.WebUtil
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/23/22.
 */
@Controller
class WebPushTestController {
    @PostMapping("/testPush")
    @ResponseBody
    fun testPush() : Int {

        //TODO: Confirm headers


        return 1
    }
}