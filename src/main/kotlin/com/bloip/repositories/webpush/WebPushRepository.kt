package com.bloip.repositories.webpush

import com.bloip.domain.webpush.WebPushSubscription
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
interface WebPushRepository : CrudRepository<WebPushSubscription, Long> {
}