package com.bloip.controllers.webpush

import com.bloip.configuration.ApplicationProperties
import com.bloip.services.webpush.WebPushService
import com.bloip.utilities.WebUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 9/17/22.
 */
@Controller
class WebPushController(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val webPushService: WebPushService
)
{
    @PostMapping("/web-push-subscription-info")
    @ResponseBody
    fun saveNewSubscription(
        @RequestParam("key") key: String,
        @RequestParam("auth") auth: String,
        @RequestParam("endpoint") endpoint: String,
        @RequestParam("expirationTime") expirationTime: String?,

        httpSession: HttpSession
    ) : Int {
        val userId: Long = WebUtil.getUserIdFromSession(httpSession)
        println("Subscription info uploaded");
        webPushService.saveNewSubscription(
            userId         = userId,
            privateKey     = key,
            auth           = auth,
            endpoint       = endpoint,
            expirationTime = expirationTime
        )

        return 1
    }
}