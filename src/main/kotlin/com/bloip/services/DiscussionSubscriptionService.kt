package com.bloip.services

/*import com.bloip.caches.DiscussionSubscriptionCache
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.DiscussionSubscription
import com.bloip.domain.discussion.DiscussionSubscriptionId
import com.bloip.domain.user.User
import com.bloip.repositories.DiscussionSubscriptionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DiscussionSubscriptionService(
    @Autowired private val discussionSubscriptionRepository: DiscussionSubscriptionRepository,
    @Autowired private val discussionSubscriptionCache: DiscussionSubscriptionCache
)
{
    fun subscribe(discussion: Discussion, user: User) {
        save(
            DiscussionSubscription(
                id = DiscussionSubscriptionId(
                    discussionId = discussionId,
                    userId       = userId
                )
            )
        )
    }

    fun unsubscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionCache.unsubscribe(discussionId = discussionId, userId = userId)
        discussionSubscriptionRepository.deleteSubscription(discussionId, userId)
    }

    fun save(subscription: DiscussionSubscription) {
        discussionSubscriptionCache.save(
            discussionSubscriptionRepository.save(subscription)
        )
    }

    fun getSubscribers(discussionId: Long) : Set<Long> {
        return discussionSubscriptionCache.getSubscribers(discussionId)
    }
}*/