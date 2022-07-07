package com.bloip.repositories.inbox

import com.bloip.domain.inbox.Notification
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
interface NotificationRepository : CrudRepository<Notification, Long> {
}