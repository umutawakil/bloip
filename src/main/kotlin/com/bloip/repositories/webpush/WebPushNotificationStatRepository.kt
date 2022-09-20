package com.bloip.repositories.webpush

import com.bloip.domain.webpush.WebPushNotificationStat
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
interface WebPushNotificationStatRepository : CrudRepository<WebPushNotificationStat, Long> {
}