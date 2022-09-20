package com.bloip.services.webpush

import com.bloip.caches.webpush.WebPushNotificationStatCache
import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.repositories.webpush.WebPushNotificationStatRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Service
class WebPushNotificationStatService(
    @Autowired private val webPushNotificationStatRepository: WebPushNotificationStatRepository,
    @Autowired private val webPushNotificationStatCache: WebPushNotificationStatCache
)
{
    fun createNewStat(userId: Long) {
        if (webPushNotificationStatCache.get(userId = userId) != null) { //If the user already has a stat then don't create another. This implies they just utilized a new device.
            return
        }
        webPushNotificationStatCache.save(
            webPushNotificationStatRepository.save(
                WebPushNotificationStat(userId = userId)
            )
        )
    }

    fun get(userId: Long) : WebPushNotificationStat? {
        return webPushNotificationStatCache.get(userId = userId)
    }
}