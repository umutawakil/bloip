package com.bloip.domain

import com.bloip.configuration.EnvironmentConfigs
import com.bloip.utilities.DiscussionUtility
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Entity
@Table(name = "comment")
class Comment : StandardDomainObject {
    @Column
    val fileName: String  //"https://www.w3schools.com/html/horse.mp3" //null

    @Column
    val trackNumber: Int

    @Column
    val userId: Long

    val discussionId: Long

    @Column
    var ipAddress:String

    @Column
    val duration: Int

    @Column
    var active = false

    @Column
    var audioConversionInProgress = false

    constructor(userId: Long, discussionId: Long, fileName: String, trackNumber: Int, ipAddress: String, duration: Int, active: Boolean) {
        this.userId        = userId
        this.discussionId  = discussionId
        this.fileName      = fileName
        this.trackNumber   = trackNumber
        this.ipAddress     = ipAddress
        this.duration      = duration
        this.active        = active
    }

    val audioUrl: String
        get() = DiscussionUtility.getPotentiallyConvertedFileLocation(this.fileName)

}