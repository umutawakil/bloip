package com.bloip.services

/*import com.bloip.caches.InboxCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.user.User
import com.bloip.repositories.InboxRepository
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service*/

/**
 * Created by Usman Mutawakil on 7/6/22.
 */

/*class InboxService (
    @Autowired private val inboxRepository: InboxRepository,
    @Autowired private val inboxCache: InboxCache,
    @Autowired private val applicationProperties: ApplicationProperties
)
{
    fun updateSubscriberInboxes(senderId: Long, discussion: Discussion, trackNumber: Int, userIds: Set<Long>) {
        for (userId in userIds) {
            if(userId == senderId) {
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

    fun updateInbox(userId: Long, discussionId: Long, trackNumber: Int, title: Title) {
        var inboxItem: InboxItem? = inboxCache.getExistingInboxConversationIfPresent(userId = userId, discussionId = discussionId)
        if (inboxItem == null) {
            //loggingService.log("Creating new inbox item for user: ${userId}")
            createNewInboxConversation(
                InboxItem(
                    userId       = userId,
                    discussionId = discussionId,
                    trackNumber  = trackNumber,
                    title        = title,
                )
            )
        } else {
            //loggingService.log("updating existing user: ${userId}")
            bumpExistingInboxConversationToTheTop(inboxItem)
        }

        /** Send notification email if applicable **/
        val updatedInboxItem = inboxCache.getInboxItem(userId = userId, discussionId = discussionId)!!
        val user: User = User.findById(userId = userId)!!
        if (updatedInboxItem.warrantsEmailNotification()) {
             user.sendDiscussionNotificationEmail()
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
        inboxCache.update(inboxItem = inboxRepository.save(inboxItem))
    }

    fun bumpExistingInboxConversationToTheTop(inboxItem: InboxItem) {
        //TODO: the repository update could be done in a future/promise
        val updatedInboxItem        = inboxCache.bumpInboxConversationToTop(inboxItem)
        inboxCache.update(inboxItem = inboxRepository.save(updatedInboxItem))
    }

    fun deleteConversation(userId: Long, discussionId: Long) {
        val inboxItem: InboxItem = inboxCache.deleteConversation(userId = userId, discussionId = discussionId) ?: return
        inboxRepository.delete(inboxItem)
    }

    fun toggleInboxSubscription(userId: Long, discussionId: Long, value: Boolean) {
        val inboxItem: InboxItem = inboxCache.toggleSubscription(userId = userId, discussionId = discussionId, value = value)
        val updatedInboxItem = inboxRepository.save(inboxItem)
        inboxCache.update(inboxItem = updatedInboxItem)
    }

    fun resetUnreadConversationIndicator(discussionId: Long, userId: Long) {
        val inboxItem: InboxItem = inboxCache.getInboxItem(userId, discussionId) ?: return
        val discussionCount: Int = inboxItem.count
        inboxItem.count = 0
        inboxItem.unread = false
        inboxCache.reduceUserInboxTotal(userId = userId, count =  discussionCount)

        inboxCache.update(inboxItem = inboxRepository.save(inboxItem))
    }

    fun getInboxItem(discussionId: Long, userId: Long) : InboxItem? {
        return inboxCache.getInboxItem(userId = userId, discussionId = discussionId)
    }
}*/