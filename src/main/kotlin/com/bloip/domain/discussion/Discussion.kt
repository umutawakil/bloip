package com.bloip.domain.discussion

import com.bloip.domain.StandardDomainObject
import com.bloip.utilities.DiscussionUtility
import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion : StandardDomainObject {
    @Embedded
    val title: Title

    @Embedded
    val youtubeLink: YoutubeLink?

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

    @Column
    var active = false

    @Column
    var audioConversionInProgress = false

    constructor(userId: Long, title: Title, ipAddress: String,fileName: String, youtubeLink: YoutubeLink? = null, active: Boolean) {
        this.userId            = userId
        this.title             = title
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
        this.fileName          = fileName
        this.youtubeLink       = youtubeLink
        this.active            = active
    }

    val audioUrl:  String
        get() = DiscussionUtility.getPotentiallyConvertedFileLocation(this.fileName)

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/d/" + this.id
    }
}