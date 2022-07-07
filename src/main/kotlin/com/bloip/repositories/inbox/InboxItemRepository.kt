package com.bloip.repositories.inbox

import com.bloip.domain.inbox.InboxItem
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
interface InboxItemRepository : PagingAndSortingRepository<InboxItem, Long> {
    @Query("SELECT i FROM InboxItem i WHERE i.inboxItemId.user.id = ?1")
    fun findByUserId(userId: Long) : List<InboxItem>
}