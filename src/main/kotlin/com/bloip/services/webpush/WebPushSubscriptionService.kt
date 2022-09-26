package com.bloip.services.webpush

import com.bloip.caches.webpush.WebPushSubscriptionCache
import com.bloip.domain.webpush.WebPushSubscription
import com.bloip.repositories.webpush.WebPushRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Service
class WebPushSubscriptionService(
    @Autowired private val webPushRepository: WebPushRepository,
    @Autowired private val webPushSubscriptionCache: WebPushSubscriptionCache,
) {

    fun deleteAll() {
        webPushRepository.deleteAll()
        webPushSubscriptionCache.init()
    }

    fun getUserSubscriptions(userId: Long) : List<WebPushSubscription> {
        return webPushSubscriptionCache.getUserSubscriptions(userId = userId)
    }

    fun save(userId: Long, privateKey: String, auth: String, endpoint: String, expirationTime: String?) : WebPushSubscription {
        val webPushSubscription = WebPushSubscription(
            userId         = userId,
            key            = privateKey,
            auth           = auth,
            endpoint       = endpoint,
            expirationTime = expirationTime
        )

        return webPushSubscriptionCache.save(
            webPushRepository.save(webPushSubscription)
        )
    }
}