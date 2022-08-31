package com.bloip.repositories

import com.bloip.domain.inbox.InboxItem
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
interface InboxRepository : CrudRepository<InboxItem, Long> {
}