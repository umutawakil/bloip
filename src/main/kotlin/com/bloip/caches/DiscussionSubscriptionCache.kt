package com.bloip.caches

/*import com.bloip.domain.discussion.DiscussionSubscription
import com.bloip.repositories.DiscussionSubscriptionRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
@Component
class DiscussionSubscriptionCache(
    @Autowired val discussionSubscriptionRepository: DiscussionSubscriptionRepository,
    @Autowired val loggingService: LoggingService
) {
    val subscriptionsByDiscussion: MutableMap<Long, MutableSet<Long>> = ConcurrentHashMap<Long, MutableSet<Long>>()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing discussion subscription cache")
        val subscriptions = discussionSubscriptionRepository.findAll()
        for(s: DiscussionSubscription in subscriptions) {
            save(s)
        }
        loggingService.log("Discussion subscription cache initialized\r\n\r\n")
    }

    fun getSubscribers(discussionId: Long) : Set<Long> {
        val temp:MutableSet<Long> = mutableSetOf()
        synchronized(subscriptionsByDiscussion) {
            for(s in (subscriptionsByDiscussion[discussionId] ?: emptySet())) {
                temp.add(s)
            }
        }
        return temp
    }

    fun save(discussionSubscription: DiscussionSubscription) {
        synchronized(subscriptionsByDiscussion) {
            var subscriptions: MutableSet<Long>? = subscriptionsByDiscussion[discussionSubscription.id.discussionId]
            if (subscriptions == null) {
                subscriptions = mutableSetOf()
                subscriptionsByDiscussion[discussionSubscription.id.discussionId] = subscriptions
            }
            subscriptions.add(discussionSubscription.id.userId)
        }
    }

    fun unsubscribe(discussionId: Long, userId: Long) {
        synchronized(subscriptionsByDiscussion) {
            val subscriptions: MutableSet<Long> = subscriptionsByDiscussion[discussionId] !!
            subscriptions.remove(userId)
        }
    }
}*/