package com.bloip.caches

import com.bloip.domain.inbox.InboxItem
import com.bloip.repositories.InboxRepository
import com.bloip.services.LoggingService
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
    private val inboxByUser: MutableMap<Long, MutableList<InboxItem>> = ConcurrentHashMap<Long, MutableList<InboxItem>>()
    private val inboxTotalsByUser: MutableMap<Long, Int?> = ConcurrentHashMap<Long, Int?>()
    private val inboxItemByUserAndDiscussion: MutableMap<String, InboxItem> = ConcurrentHashMap<String, InboxItem>()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing inbox cache")

        for(n: InboxItem in inboxRepository.findAll()) {
            var localInboxItems: MutableList<InboxItem>? = inboxByUser[n.userId]
            if(localInboxItems == null) {
                localInboxItems = Collections.synchronizedList(ArrayList())!!
                inboxByUser[n.userId] = localInboxItems
            }
            localInboxItems.add(n)
            inboxItemByUserAndDiscussion["${n.userId}-${n.discussionId}"] = n
        }

        for(userId: Long in inboxByUser.keys) {
            inboxTotalsByUser[userId] = calculateInboxTotal(userId = userId)
        }
        loggingService.log("Inbox cache initialized.\r\n\r\n")
    }

    fun getUserTotal(userId: Long) : Int {
        return inboxTotalsByUser[userId] ?: throw RuntimeException("User id not found when getting total")
    }

    //TODO: Think of the situations in which this needs to be ran again
    fun calculateInboxTotal(userId: Long) : Int {
        val notes: MutableList<InboxItem> = inboxByUser[userId] ?: return 0
        return notes.fold(
            0,
            {acc, inboxItem -> acc + inboxItem.count }
        )
    }

    fun getInboxItem(userId: Long, discussionId: Long) : InboxItem? {
        return inboxItemByUserAndDiscussion["${userId}-${discussionId}"]
    }

    fun getInbox(userId: Long) : List<InboxItem>? {
        return inboxByUser[userId]
    }

    fun addNewEntry(inboxItem: InboxItem) {
        inboxItemByUserAndDiscussion["${inboxItem.userId}-${inboxItem.discussionId}"] = inboxItem

        val inbox:MutableList<InboxItem> = inboxByUser[inboxItem.userId] ?: mutableListOf()
        if(inbox.isEmpty()) {
            inboxByUser[inboxItem.userId] = inbox
        }
        inbox.add(inboxItem)
        inbox.sortByDescending { it.lastUpdateTimestamp }

        inboxTotalsByUser[inboxItem.userId] = inboxTotalsByUser[inboxItem.userId]?.plus(1)
    }

    fun incrementInbox(inboxItem: InboxItem) : InboxItem {
        inboxItem.count++
        inboxItem.lastUpdateTimestamp = Date()

        val inbox:MutableList<InboxItem> = inboxByUser[inboxItem.userId] ?: mutableListOf()
        inbox.sortByDescending { it.lastUpdateTimestamp }

        inboxTotalsByUser[inboxItem.userId] = inboxTotalsByUser[inboxItem.userId]?.plus(1)

        return inboxItem
    }
}