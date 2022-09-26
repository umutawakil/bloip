package com.bloip.services.webpush

import com.bloip.caches.webpush.WebPushNotificationStatCache
import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.repositories.webpush.WebPushNotificationStatRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.Timestamp

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Service
class WebPushNotificationStatService(
    @Autowired private val webPushNotificationStatRepository: WebPushNotificationStatRepository,
    @Autowired private val webPushNotificationStatCache: WebPushNotificationStatCache
)
{
    fun deleteAll() {
        webPushNotificationStatRepository.deleteAll()
        webPushNotificationStatCache.init()
    }

    fun createNewStat(userId: Long) {
        if (webPushNotificationStatCache.get(userId = userId) != null) { //If the user already has a stat then don't create another. This implies they just utilized a new device.
            return
        }

        save(
            WebPushNotificationStat(userId = userId)
        )
    }

    fun get(userId: Long) : WebPushNotificationStat? {
        return webPushNotificationStatCache.get(userId = userId)
    }

    fun scheduleWebPush(userIds: Collection<Long>) {
        for(userId in userIds) {
            scheduleWebPush(userId)
        }
    }

    private fun scheduleWebPush(userId: Long) {
        val webPushNotificationStat = webPushNotificationStatCache.get(userId = userId) ?: return
        webPushNotificationStat.needsNotification = true

        save(webPushNotificationStat)
        webPushNotificationStatCache.scheduleWebPush(webPushNotificationStat)
    }

    fun save(webPushNotificationStat: WebPushNotificationStat) {
        webPushNotificationStat.lastUpdateTimestamp = Timestamp(System.currentTimeMillis())
        webPushNotificationStatCache.save(
            webPushNotificationStatRepository.save(webPushNotificationStat)
        )
    }

    fun getAllThatNeedNotification() : Collection<WebPushNotificationStat> {
        return webPushNotificationStatCache.getAllThatNeedNotification()
    }

    fun markNotificationStatAsProcessed(webPushNotificationStat: WebPushNotificationStat)  {
        webPushNotificationStatCache.descheduleWebPush(webPushNotificationStat)

        webPushNotificationStat.dailyCount++
        webPushNotificationStat.totalSent++
        webPushNotificationStat.needsNotification = false
        save(webPushNotificationStat)
    }
}