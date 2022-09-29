package com.bloip.domain.inbox

import com.bloip.domain.StandardDomainObject
import com.bloip.domain.discussion.Title
import java.util.*
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Entity
@Table(name = "inbox")
class InboxItem : StandardDomainObject{
    @Column(name = "discussion_id")
    val discussionId: Long

    @Column(name = "user_id")
    val userId: Long

    @Embedded
    val title: Title

    @Column
    val trackNumber: Int

    @Column
    var count: Int

    @Column
    var subscribed:Boolean

    @Column
    var lastUpdateTimestamp: Date

    @Column
    val creationTimestamp: Date

    @Column
    var unread: Boolean

    constructor(userId: Long, discussionId: Long, trackNumber: Int, title: Title) {
        this.userId              = userId
        this.discussionId        = discussionId
        this.title               = title
        this.trackNumber         = trackNumber
        this.count               = 1
        this.lastUpdateTimestamp = Date()
        this.creationTimestamp   = Date()
        this.subscribed          = true
        this.unread              = true
    }
}