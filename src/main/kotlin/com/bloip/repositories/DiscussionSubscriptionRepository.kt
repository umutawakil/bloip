package com.bloip.repositories

import com.bloip.domain.DiscussionSubscription
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
interface DiscussionSubscriptionRepository : CrudRepository<DiscussionSubscription, Long> {
    @Modifying
    @Query("DELETE FROM DiscussionSubscription ds WHERE ds.id.discussionId = ?1 AND ds.id.userId= ?2", )
    fun deleteSubscription(discussionId: Long, userId: Long)
}