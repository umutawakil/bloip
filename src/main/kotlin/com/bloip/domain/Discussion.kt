package com.bloip.domain

import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion : StandardDomainObject {
    @Column
    val title: String

    @Column
    val audioUrl: String? = "https://www.w3schools.com/html/horse.mp3"//null

    @Column
    val ipAddress: String

    @Column
    var numberOfReplies: Int = 0

    @Column
    val creationTimestamp: Date

    @Column
    var updateTimestamp: Date

    @Column
    val userId: Long

    @OneToOne(optional = true)
    @JoinColumn(name = "topic_id", referencedColumnName = "id", nullable = true)
    val topic: Topic

    constructor(userId: Long, title: String, topic: Topic, ipAddress: String) {
        this.userId            = userId
        this.title             = title
        this.topic             = topic
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
    }

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/d/" + this.id
    }
}