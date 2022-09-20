package com.bloip.caches.webpush

import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.repositories.webpush.WebPushNotificationStatRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Component
class WebPushNotificationStatCache (
    @Autowired val webPushNotificationStatRepository: WebPushNotificationStatRepository,
    @Autowired val loggingService: LoggingService
) {
    private val stats: MutableMap<Long, WebPushNotificationStat> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        loggingService.log("\r\nLoading web push notification cache")

        for(p in webPushNotificationStatRepository.findAll()) {
            save(p)
        }
        loggingService.log("Loaded ${stats.size} web push notification stats.\r\n")
    }

    fun save(webPushNotificationStat: WebPushNotificationStat) : WebPushNotificationStat {
        stats[webPushNotificationStat.userId] = webPushNotificationStat
        return webPushNotificationStat
    }

    fun get(userId: Long) : WebPushNotificationStat? {
        return stats[userId]
    }

    fun getAll() : Collection<WebPushNotificationStat> {
        return stats.values
    }
}