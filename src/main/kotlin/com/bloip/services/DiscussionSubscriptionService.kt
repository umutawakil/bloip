package com.bloip.services

import com.bloip.caches.DiscussionSubscriptionCache
import com.bloip.domain.discussion.DiscussionSubscription
import com.bloip.domain.discussion.DiscussionSubscriptionId
import com.bloip.repositories.DiscussionSubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
@Service
class DiscussionSubscriptionService(
    @Autowired private val discussionSubscriptionRepository: DiscussionSubscriptionRepository,
    @Autowired private val discussionSubscriptionCache: DiscussionSubscriptionCache
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

    @Transactional
    fun unsubscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionCache.unsubscribe(discussionId = discussionId, userId = userId)
        discussionSubscriptionRepository.deleteSubscription(discussionId, userId)
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