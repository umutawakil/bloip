package com.bloip.domain.inbox

import java.util.*
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Entity
@Table(name = "notification")
class Notification {
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
    val creationTimestamp: Date = Date()

    constructor(userId: Long, discussionId: Long, title: String) {
        this.userId       = userId
        this.discussionId = discussionId
        this.title        = title
    }
}