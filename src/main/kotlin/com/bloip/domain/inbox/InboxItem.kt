package com.bloip.domain.inbox

import java.util.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */

class InboxItem {
    val userId: Long
    val discussionId: Long
    var count:Int
    val title: String
    val creationTimestamp: Date

    constructor(discussionId: Long, userId: Long, count: Int, title: String, creationTimestamp: Date) {
        this.discussionId      = discussionId
        this.userId            = userId
        this.count             = count
        this.title             = title
        this.creationTimestamp = creationTimestamp
    }
}