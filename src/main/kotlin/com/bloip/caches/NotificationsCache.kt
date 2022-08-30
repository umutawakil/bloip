package com.bloip.caches

import com.bloip.domain.inbox.Notification
import com.bloip.repositories.NotificationRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class NotificationsCache(
    @Autowired val notificationsRepository: NotificationRepository,
    @Autowired val loggingService: LoggingService
)
{
    private val notificationsByUser: MutableMap<Long, MutableList<Notification>> = ConcurrentHashMap<Long, MutableList<Notification>>()

    @PostConstruct
    fun init() {
        loggingService.log("\r\n\r\nInitializing notifications cache")

        for(n: Notification in notificationsRepository.findAll()) {
            var localNotifications: MutableList<Notification>? = notificationsByUser[n.userId]
            if(localNotifications == null) {
                localNotifications = ArrayList()
                notificationsByUser[n.userId] = localNotifications
            }
            localNotifications.add(n)
        }
        loggingService.log("Notifications cache initialized.\r\n\r\n")
    }

    fun getUserTotal(userId: Long) : Int {
        val notes: MutableList<Notification> = notificationsByUser[userId] ?: return 0
        return notes.size
    }

    fun getUserNotifications(userId: Long) : List<Notification>? {
        return notificationsByUser[userId]
    }
}