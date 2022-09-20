package com.bloip.services.webpush

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Service
class WebPushService(
    @Autowired private val webPushSubscriptionService: WebPushSubscriptionService,
    @Autowired private val webPushNotificationStatService: WebPushNotificationStatService
) {

    @Transactional
    fun saveNewSubscription(userId: Long, privateKey: String, auth: String, endpoint: String, expirationTime: String?) {
        webPushSubscriptionService.save(
            userId         = userId,
            privateKey     = privateKey,
            auth           = auth,
            endpoint       = endpoint,
            expirationTime = expirationTime
        )
        webPushNotificationStatService.createNewStat(userId = userId)
    }

    fun scheduleWebPush(userIds: Collection<Long>) {
        for(userId in userIds) {
            webPushNotificationStatService.get(userId = userId)?.needsNotification = true
        }
    }

    fun sendAllPendingWebPushNotifications() {
       // for(s in webPushNotificationStatCache.getAll()) {
            //TODO: Remember to replace the cache value.

            //TODO: If count is 0 increment and send

            //TODO: If time > 24 reset to 1 and send

            //TODO: If count > Max ignore an return

            //TODO: If time stamp is < min_notification_threshold ignore and return

            //TODO: Increment count and send
       // }
    }
}