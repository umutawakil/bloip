package com.bloip.caches.webpush

import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.repositories.webpush.WebPushNotificationStatRepository
import com.bloip.services.LoggingService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import kotlin.collections.HashSet

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Component
class WebPushNotificationStatCache (
    @Autowired val webPushNotificationStatRepository: WebPushNotificationStatRepository,
    @Autowired val loggingService: LoggingService
) {
    private var stats: MutableMap<Long, WebPushNotificationStat>       = ConcurrentHashMap()
    private var needsNotification: MutableSet<WebPushNotificationStat> = Collections.synchronizedSet(HashSet())

    @PostConstruct
    fun init() {
        loggingService.log("\r\nLoading web push notification cache")
        stats             = ConcurrentHashMap()
        needsNotification = HashSet()

        for (p in webPushNotificationStatRepository.findAll()) {
            if (p.needsNotification) {
                needsNotification.add(p)
            }
            save(p)
        }
        loggingService.log("Loaded ${stats.size} web push notification stats.\r\n")
    }

    fun save(webPushNotificationStat: WebPushNotificationStat) : WebPushNotificationStat {
        synchronized(this.stats) {
            stats[webPushNotificationStat.userId] = webPushNotificationStat
        }

        return webPushNotificationStat
    }

    fun scheduleWebPush(webPushNotificationStat: WebPushNotificationStat) {
        synchronized(this.stats) {
            stats[webPushNotificationStat.userId] = webPushNotificationStat
        }

        synchronized(this.needsNotification) {
            needsNotification.add(webPushNotificationStat)
        }
    }

    fun descheduleWebPush(webPushNotificationStat: WebPushNotificationStat) {
        synchronized(this.needsNotification) {
            needsNotification.remove(webPushNotificationStat)
        }
    }

    fun get(userId: Long) : WebPushNotificationStat? {
        return stats[userId]
    }

    fun getAllThatNeedNotification() : Set<WebPushNotificationStat> {
        synchronized(needsNotification) {
            val temp:MutableSet<WebPushNotificationStat> = mutableSetOf()
            for(s in needsNotification) {
                temp.add(s)
            }
            return temp
        }
    }
}