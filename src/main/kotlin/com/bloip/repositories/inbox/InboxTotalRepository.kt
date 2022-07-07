package com.bloip.repositories.inbox

import com.bloip.domain.inbox.InboxTotal
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
interface InboxTotalRepository : CrudRepository<InboxTotal, Long> {
    fun findByUserId(userId: Long) : List<InboxTotal>
}