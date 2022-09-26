package com.bloip.jobs.webpush

import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/19/22.
 */

//TODO: Job is not in use as web push doesn't seem worth it at the moment.
class WebPushRunner (
    @Autowired private val webPushNotificationSender: WebPushNotificationSender,
    @Autowired private val loggingService: LoggingService
)
{
    @PostConstruct
    fun init() {
        if(System.getenv("VAPID_PRIVATE_KEY")!= null) {
            loggingService.log("VAPID private key found so WebPushRunner will be executed")
            Thread(webPushNotificationSender).start()
        } else {
            loggingService.log("WARNING: VAPID private key NOT found so WebPushRunner will NOT be executed. If running unit test this is Okay and expected. Otherwise you need help.")
        }
    }
}