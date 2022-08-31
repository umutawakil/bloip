package com.bloip.repositories

import com.bloip.domain.DiscussionSubscription
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
interface DiscussionSubscriptionRepository : CrudRepository<DiscussionSubscription, Long> {
}