package com.bloip.services

import com.bloip.domain.User
import com.bloip.domain.inbox.Notification
import com.bloip.repositories.inbox.NotificationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Service
class NotificationService (
    @Autowired val userService: UserService,
    @Autowired val notificationRepository: NotificationRepository
)
{
    @Transactional
    fun notifyAll(senderId: Long, discussionId: Long) {
        val users: List<User> = userService.findAllUsersInDiscussion(discussionId)

        for ( user:User in users) {
            if(user.id == senderId) {
                continue
            }

            notify(
                userId       = user.id!!,
                discussionId = discussionId
            )
        }
    }

    private fun notify(userId: Long, discussionId: Long) {
        notificationRepository.save(
            Notification(
                userId       = userId,
                discussionId = discussionId
            )
        )
    }
}