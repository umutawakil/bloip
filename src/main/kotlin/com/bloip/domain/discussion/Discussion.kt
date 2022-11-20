package com.bloip.domain.discussion

import com.bloip.domain.localization.Country
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.localization.Language
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
    var lastUserId: Long

    @Column
    val fileName: String

    @Column
    var needsConversion: Boolean

    @Column
    var audioConversionInProgress = false

    @Column
    var conversionJobId: String? = null

    @ManyToOne(optional = true)
    @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false)
    val country: Country

    constructor(
        userId: Long,
        title: Title,
        ipAddress: String,
        fileName: String,
        youtubeLink: YoutubeLink? = null,
        needsConversion: Boolean,
        country: Country
    ) {
        this.userId            = userId
        this.lastUserId        = userId
        this.title             = title
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
        this.fileName          = fileName
        this.youtubeLink       = youtubeLink
        this.needsConversion   = needsConversion
        this.country           = country
    }

    val audioUrl:  String
        get() = DiscussionUtility.getPotentiallyConvertedFileLocation(
            needsConversion = this.needsConversion,
            fileName        = this.fileName
        )

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(language: Language): String {
        return "/d/" + this.id + "/l/" + language.code
    }
}