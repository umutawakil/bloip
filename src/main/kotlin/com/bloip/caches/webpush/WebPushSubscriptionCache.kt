package com.bloip.caches.webpush

import com.bloip.domain.webpush.WebPushSubscription
import com.bloip.repositories.webpush.WebPushRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Component
class WebPushSubscriptionCache(
    @Autowired val webPushRepository: WebPushRepository,
    @Autowired val loggingService: LoggingService
) {
    private val subscriptionsByUser: MutableMap<Long, MutableList<WebPushSubscription>> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        loggingService.log("\r\nLoading web push subscription cache")

        for(p in webPushRepository.findAll()) {
            save(p)
        }
        loggingService.log("Loaded subscriptions for ${subscriptionsByUser.size} users.\r\n")
    }

    fun getUserSubscriptions(userId: Long) : List<WebPushSubscription> {
        return subscriptionsByUser[userId] ?: emptyList()
    }

    fun save(webPushSubscription: WebPushSubscription) : WebPushSubscription {
        subscriptionsByUser.computeIfAbsent(webPushSubscription.userId) { mutableListOf() }.add(webPushSubscription)
        return webPushSubscription
    }
}