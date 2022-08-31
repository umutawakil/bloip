package com.bloip.services

import com.bloip.caches.DiscussionSubscriptionCache
import com.bloip.domain.DiscussionSubscription
import com.bloip.domain.DiscussionSubscriptionId
import com.bloip.repositories.DiscussionSubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
@Service
class DiscussionSubscriptionService(
    @Autowired val discussionSubscriptionRepository: DiscussionSubscriptionRepository,
    @Autowired val discussionSubscriptionCache: DiscussionSubscriptionCache,
    @Autowired val loggingService: LoggingService
)
{
    fun subscribe(discussionId: Long, userId: Long) {
        save(
            DiscussionSubscription(
                id = DiscussionSubscriptionId(
                    discussionId = discussionId,
                    userId       = userId
                )
            )
        )
    }

    fun save(subscription: DiscussionSubscription): DiscussionSubscription? {
        if(discussionSubscriptionCache.save(subscription)) {
            return discussionSubscriptionRepository.save(subscription)
        }
        return null
    }

    fun getSubscribers(discussionId: Long) : Set<Long> {
        return discussionSubscriptionCache.getSubscribers(discussionId) ?: emptySet()
    }
}