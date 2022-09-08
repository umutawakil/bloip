package com.bloip.caches

import com.bloip.domain.inbox.InboxItem
import com.bloip.repositories.InboxRepository
import com.bloip.services.LoggingService
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import kotlin.collections.ArrayList

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class InboxCache(
    @Autowired val inboxRepository: InboxRepository,
    @Autowired val loggingService: LoggingService
)
{
    private val inboxTotalsByUser: MutableMap<Long, Int?> = ConcurrentHashMap<Long, Int?>()
    private val inboxStackByUser: MutableMap<Long, BumpStack<Long, InboxItem>> = ConcurrentHashMap<Long, BumpStack<Long, InboxItem>>()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing inbox cache")

        val inboxByUser: MutableMap<Long, MutableList<InboxItem>> = ConcurrentHashMap<Long, MutableList<InboxItem>>()
        for(n: InboxItem in inboxRepository.findAll()) {
            /** Remember not every user has an inbox and once its created where folding the existing inbox.
             *  It could have been cleared or they have no conversations **/

            var localInboxItems: MutableList<InboxItem>? = inboxByUser[n.userId]
            if(localInboxItems == null) {
                localInboxItems = Collections.synchronizedList(ArrayList())!!
                inboxByUser[n.userId] = localInboxItems
            }
            localInboxItems.add(n)

        }
        for(userId: Long in inboxByUser.keys) {
            val inboxStack = BumpStack<Long, InboxItem>()
            inboxByUser[userId]?.sortedBy { x -> x.lastUpdateTimestamp }?.forEach { x ->
                inboxStack.push(x.discussionId, x)
            }
            inboxStackByUser[userId] = inboxStack
        }

        for(userId: Long in inboxByUser.keys) {
            inboxTotalsByUser[userId] = calculateInboxTotal(userId = userId)
        }
        loggingService.log("Inbox cache initialized\r\n\r\n")
    }

    private fun calculateInboxTotal(userId: Long) : Int {
        val notes: List<InboxItem> = inboxStackByUser[userId]?.getAll() ?: return 0
        return notes.fold(
            0,
            {acc, inboxItem -> acc + inboxItem.count }
        )
    }

    fun getUserTotal(userId: Long) : Int {
        return inboxTotalsByUser[userId] ?: 0
    }

    fun getExistingInboxConversationIfPresent(userId: Long, discussionId: Long) : InboxItem? {
        return inboxStackByUser[userId]?.get(key = discussionId)
    }

    fun getInbox(userId: Long) : BumpStack<Long, InboxItem>? {
        return inboxStackByUser[userId]
    }

    fun getInboxItem(userId: Long, discussionId: Long) : InboxItem? {
        return inboxStackByUser[userId]?.get(key = discussionId)
    }

    /** AddNewEntry and incrementInbox are similar except addNewEntry creates a new row in the inbox
     * and incrementInbox moves it down and up **/

    fun createNewInboxConversation(inboxItem: InboxItem) {
        var inbox: BumpStack<Long, InboxItem> = inboxStackByUser[inboxItem.userId] ?: BumpStack<Long, InboxItem>()
        if(inbox.size() == 0) {
            inboxStackByUser[inboxItem.userId] = inbox
        }
        inbox.push(key = inboxItem.discussionId, element = inboxItem)
        incrementUserInboxTotal(userId = inboxItem.userId)
    }

    fun bumpInboxConversationToTop(inboxItem: InboxItem) : InboxItem {
        inboxItem.count++
        val inbox:BumpStack<Long, InboxItem> = inboxStackByUser[inboxItem.userId]!!
        inbox.bump(key = inboxItem.discussionId)
        incrementUserInboxTotal(userId = inboxItem.userId)

        return inboxItem
    }

    private fun incrementUserInboxTotal(userId: Long) {
        inboxTotalsByUser[userId] = (inboxTotalsByUser[userId]?: 0) + 1
    }

    fun deleteConversation(userId: Long, discussionId: Long) : InboxItem? {
        val inbox: BumpStack<Long, InboxItem> = inboxStackByUser[userId] ?: return null
        val inboxItem: InboxItem = inbox.get(key = discussionId) ?: return null
        inbox.remove(key = discussionId)
        reduceUserInboxTotal(userId = userId, count = inboxItem.count)

        return inboxItem
    }

    //When applied to unread inbox items the count is only unread items so there should be litte fear of getting out of sync
    fun reduceUserInboxTotal(userId: Long, count: Int) {
        inboxTotalsByUser[userId] = inboxTotalsByUser[userId]?.minus(count)
    }

    fun toggleSubscription(userId: Long, discussionId: Long, value: Boolean) : InboxItem?{
        val inboxItem: InboxItem = inboxStackByUser[userId]?.get(key = discussionId) ?: return null
        inboxItem.subscribed = value
        return inboxItem
    }
}