package com.bloip.domain.webpush

import com.bloip.domain.StandardDomainObject
import java.sql.Timestamp
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Entity
@Table(name = "web_push_notification_stat")
class WebPushNotificationStat : StandardDomainObject {
    val userId: Long
    var dailyCount                 = 0
    var lastUpdateTimestamp: Timestamp
    var totalSent                  = 0
    var totalClicked               = 0
    var totalReceived              = 0
    var needsNotification: Boolean = false

    constructor(userId: Long) {
        this.userId = userId
        this.lastUpdateTimestamp = Timestamp(System.currentTimeMillis())
    }
}
