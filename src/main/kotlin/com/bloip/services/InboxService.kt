package com.bloip.services

import com.bloip.domain.inbox.InboxItem
import com.bloip.domain.inbox.InboxTotal
import com.bloip.repositories.inbox.InboxItemRepository
import com.bloip.repositories.inbox.InboxTotalRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Service
class InboxService  (
    @Autowired val inboxItemRepository: InboxItemRepository,
    @Autowired val inboxTotalRepository: InboxTotalRepository
)
{
    fun getInboxTotal(userId: Long) : Int {
        val inboxTotal: InboxTotal = CollectionUtils.firstElement(inboxTotalRepository.findByUserId(userId)) ?: return 0
        return inboxTotal.total
    }

    fun getInbox(userId: Long) : List<InboxItem> {
        return inboxItemRepository.findByUserId(userId)
    }
}