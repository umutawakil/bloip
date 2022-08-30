package com.bloip.services

import com.bloip.caches.NotificationsCache
import com.bloip.domain.Discussion
import com.bloip.domain.User
import com.bloip.domain.inbox.InboxItem
import com.bloip.domain.inbox.Notification
import com.bloip.repositories.NotificationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.streams.toList

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Service
class NotificationService (
    @Autowired val notificationRepository: NotificationRepository,
    @Autowired val notificationsCache: NotificationsCache
)
{
    @Transactional
    fun notifyAll(senderId: Long, discussion: Discussion) {
        for ( user:User in getUsers(discussion)) {
            if(user.id == senderId) {
                continue
            }

            notify(
                userId       = user.id,
                discussionId = discussion.id,
                title        = discussion.title
            )
        }
    }

    //TODO: Is there a cleaner way that doesn't dive so deep?
    private fun getUsers(discussion: Discussion) : List<User> {
        return discussion.comments.stream().map { it.user}.toList()
    }

    private fun notify(userId: Long, discussionId: Long, title: String) {
        notificationRepository.save(
            Notification(
                userId       = userId,
                discussionId = discussionId,
                title        = title
            )
        )
    }

    fun getInboxTotal(userId: Long) : Int {
        return notificationsCache.getUserTotal(userId)
    }

    fun getInbox(userId: Long) : List<InboxItem> {
        val notifications: List<Notification> = notificationsCache.getUserNotifications(userId) ?: return emptyList()
        val map: MutableMap<Long, InboxItem> = HashMap()

        for(n: Notification in notifications) {
            var item: InboxItem? = map[n.discussionId]
            if(item == null) {
                item = InboxItem(
                    discussionId      = n.discussionId,
                    userId            = n.userId,
                    count             = 0,
                    title             = n.title,
                    creationTimestamp = n.creationTimestamp
                )
                map[n.discussionId] = item
            }
            item.count++
        }
        return map.values.sortedByDescending { it.creationTimestamp }
    }

}