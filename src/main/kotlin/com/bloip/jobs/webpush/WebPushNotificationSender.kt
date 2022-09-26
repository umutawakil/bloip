package com.bloip.jobs.webpush

import com.bloip.configuration.ApplicationProperties
import com.bloip.services.LoggingService
import com.bloip.services.webpush.WebPushService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Created by Usman Mutawakil on 9/19/22.
 */
@Component
class WebPushNotificationSender(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val webPushService: WebPushService,
    @Autowired private val loggingService: LoggingService
) : Runnable {

    override fun run() {
        //while(true) {
            loggingService.log("Checking for notifications that need to be sent....")
            Thread.sleep(applicationProperties.webPushRunnerCheckTime)
            webPushService.sendAllPendingWebPushNotifications()
       // }
    }
}