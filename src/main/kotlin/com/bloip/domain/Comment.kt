package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Entity
@Table(name = "comment")
class Comment : StandardDomainObject {
    @Column
    val audioUrl: String  //"https://www.w3schools.com/html/horse.mp3" //null

    @Column
    val trackNumber: Int

    @Column
    val userId: Long

    val discussionId: Long

    @Column
    var ipAddress:String

    constructor(userId: Long, discussionId: Long, audioUrl: String, trackNumber: Int, ipAddress: String) {
        this.userId        = userId
        this.discussionId  = discussionId
        this.audioUrl      = audioUrl
        this.trackNumber   = trackNumber
        this.ipAddress     = ipAddress
    }
}