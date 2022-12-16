package com.bloip.domain.discussion

import com.bloip.configuration.EnvironmentConfigs
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
    var title: Title

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
    var fileName: String

    @Column
    var needsConversion: Boolean

    @Column
    var audioConversionInProgress = false

    @Column
    var conversionJobId: String? = null

    @Column
    var censured: Boolean = false

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

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    val audioUrl:  String
        get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
            needsConversion = this.needsConversion,
            fileName        = this.fileName
        ) } else {
            EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
        }

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(language: Language): String {
        return "/d/" + this.id + "/l/" + language.code
    }

    fun getEnglishUrl() : String {
        return "/d/" + this.id + "/l/en"
    }
}