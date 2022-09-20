package com.bloip.services

import com.bloip.caches.InboxCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.domain.inbox.InboxItem
import com.bloip.repositories.InboxRepository
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Service
class InboxService (
    @Autowired private val inboxRepository: InboxRepository,
    @Autowired private val inboxCache: InboxCache,
    @Autowired private val userService: UserService,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val applicationProperties: ApplicationProperties
)
{
    @Transactional
    fun updateSubscriberInboxes(senderId: Long, discussion: Discussion, trackNumber: Int, userIds: Set<Long>) {
        for ( userId in userIds) {
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
            loggingService.debug("Creating new inbox item for user: ${userId}")
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
            loggingService.debug("updating existing user: ${userId}")
            bumpExistingInboxConversationToTheTop(inboxItem)
        }
    }

    fun getInboxTotal(userId: Long) : Int {
        return inboxCache.getUserTotal(userId)
    }

    fun getNextPage(userId: Long, offsetKey: Long?) : BumpStack.Page<Long, InboxItem> {
        return inboxCache.getInbox(userId = userId)?.nextPage(inputKey = offsetKey, N = applicationProperties.inboxItemsPerPage)
            ?: return BumpStack.Page(null, null, emptyList())

    }
    fun getPreviousPage(userId: Long, offsetKey: Long) : BumpStack.Page<Long, InboxItem> {
        return inboxCache.getInbox(userId = userId)?.previousPage(inputKey = offsetKey, N = applicationProperties.inboxItemsPerPage)
            ?: return BumpStack.Page(null, null, emptyList())
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

    fun deleteConversation(userId: Long, discussionId: Long) {
        val inboxItem: InboxItem = inboxCache.deleteConversation(userId = userId, discussionId = discussionId) ?: return
        inboxRepository.delete(inboxItem)
    }

    fun toggleInboxSubscriptionIfInboxItemExists(userId: Long, discussionId: Long, value: Boolean) {
        val inboxItem: InboxItem = inboxCache.toggleSubscription(userId = userId, discussionId = discussionId, value = value)
            ?: return
        inboxRepository.save(inboxItem)
    }

    fun resetUnreadConversationIndicator(discussionId: Long, userId: Long) {
        val inboxItem: InboxItem = inboxCache.getInboxItem(userId, discussionId) ?: return
        val discussionCount: Int = inboxItem.count
        inboxItem.count = 0
        inboxItem.unread = false
        inboxCache.reduceUserInboxTotal(userId = userId, count =  discussionCount)

        inboxRepository.save(inboxItem)
    }
}