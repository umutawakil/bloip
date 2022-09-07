package com.bloip.domain.inbox

import java.util.*
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Entity
@Table(name = "inbox")
class InboxItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column(name = "discussion_id")
    val discussionId: Long

    @Column(name = "user_id")
    val userId: Long

    @Column(name= "title")
    val title: String

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

    constructor(userId: Long, discussionId: Long, trackNumber: Int, title: String) {
        this.userId              = userId
        this.discussionId        = discussionId
        this.title               = title
        this.trackNumber         = trackNumber
        this.count               = 1
        this.lastUpdateTimestamp = Date()
        this.creationTimestamp   = Date()
        this.subscribed          = true
    }
}