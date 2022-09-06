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
    @Autowired val discussionSubscriptionService: DiscussionSubscriptionService,
    @Autowired val userService: UserService,
    @Autowired val loggingService: LoggingService
)
{
    @Transactional
    fun updateSubscriberInboxes(senderId: Long, discussion: Discussion, trackNumber: Int) {
        for ( userId in discussionSubscriptionService.getSubscribers(discussionId = discussion.id)) {
            if(userId == senderId || userService.isNotActiveUser(userId)) {
                continue
            }

            updateInbox(
                userId       = userId,
                discussionId = discussion.id,
                trackNumber  = trackNumber,
                title        = discussion.title
            )
        }
    }

    private fun updateInbox(userId: Long, discussionId: Long, trackNumber: Int, title: String) {
        var inboxItem: InboxItem? = inboxCache.getExistingInboxConversationIfPresent(userId = userId, discussionId = discussionId)
        if (inboxItem == null) {
            loggingService.log("Creating new inbox item for user: ${userId}")
            createNewInboxConversation(
                InboxItem(
                    userId       = userId,
                    discussionId = discussionId,
                    trackNumber  = trackNumber,
                    title        = title,
                )
            )
            return
        } else {
            loggingService.log("updating existing user: ${userId}")
            bumpExistingInboxConversationToTheTop(inboxItem)
        }
    }

    fun getInboxTotal(userId: Long) : Int {
        return inboxCache.getUserTotal(userId)
    }

    fun getInbox(userId: Long) : List<InboxItem> {
       return inboxCache.getInbox(userId) ?: return emptyList()
    }

    //TODO: Needs a transaction of some sort
    fun createNewInboxConversation(inboxItem: InboxItem) {
        inboxCache.createNewInboxConversation(inboxItem)
        inboxRepository.save(inboxItem)
    }

    fun bumpExistingInboxConversationToTheTop(inboxItem: InboxItem) {
        //TODO: the repository update could be done in a future/promise
        val updatedInboxItem = inboxCache.bumpInboxConversationToTop(inboxItem)
        inboxRepository.save(updatedInboxItem)
    }
}