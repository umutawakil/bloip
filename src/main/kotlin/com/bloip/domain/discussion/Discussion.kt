package com.bloip.domain.discussion

import com.bloip.configuration.EnvironmentConfigs
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.Topic
import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion : StandardDomainObject {
    @Embedded
    val title: Title

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

    @Column
    val fileName: String

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "topic_id", referencedColumnName = "id", nullable = true)
    val topic: Topic

    constructor(userId: Long, title: Title, topic: Topic, ipAddress: String,fileName: String) {
        this.userId            = userId
        this.title             = title
        this.topic             = topic
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
        this.fileName          = fileName
    }

    val audioUrl: String
        get() = EnvironmentConfigs.audioCdnRootUrl + "/" + this.fileName

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/d/" + this.id
    }
}