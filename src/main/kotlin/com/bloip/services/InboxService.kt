package com.bloip.services

import com.bloip.caches.InboxCache
import com.bloip.domain.Discussion
import com.bloip.domain.inbox.InboxItem
import com.bloip.repositories.InboxRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Service
class InboxService (
    @Autowired val inboxRepository: InboxRepository,
    @Autowired val inboxCache: InboxCache,
    @Autowired val discussionSubscriptionService: DiscussionSubscriptionService
)
{
    @Transactional
    fun sendToAll(senderId: Long, discussion: Discussion, trackNumber: Int) {
        for ( userId in discussionSubscriptionService.getSubscribers(discussionId = discussion.id)) {
            if(userId == senderId) {
                continue
            }

            send(
                userId       = userId,
                discussionId = discussion.id,
                trackNumber  = trackNumber,
                title        = discussion.title
            )
        }
    }

    private fun send(userId: Long, discussionId: Long, trackNumber: Int, title: String) {
        var inboxItem: InboxItem? = inboxCache.getInboxItem(userId = userId, discussionId = discussionId)
        if (inboxItem == null) {
            saveNew(
                InboxItem(
                    userId       = userId,
                    discussionId = discussionId,
                    trackNumber  = trackNumber,
                    title        = title,
                )
            )
            return
        } else {
            //TODO: the repository update could be done in a future/promise
            val updatedInboxItem = inboxCache.incrementInbox(inboxItem)
            inboxRepository.save(updatedInboxItem)
        }
    }

    fun getInboxTotal(userId: Long) : Int {
        return inboxCache.getUserTotal(userId)
    }

    fun getInbox(userId: Long) : List<InboxItem> {
       return inboxCache.getInbox(userId) ?: return emptyList()
    }

    //TODO: Needs a transaction of some sort
    fun saveNew(inboxItem: InboxItem) {
        inboxCache.addNewEntry(inboxItem)
        inboxRepository.save(inboxItem)
    }

}