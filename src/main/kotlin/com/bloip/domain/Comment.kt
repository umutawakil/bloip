package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Entity
@Table(name = "comment")
class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

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
        this.userId          = userId
        this.discussionId  = discussionId
        this.audioUrl      = audioUrl
        this.trackNumber   = trackNumber
        this.ipAddress     = ipAddress
    }

    override fun equals(inputOtherObject: Any?) : Boolean {
        if (inputOtherObject == null) {
            throw NullPointerException("Equal comparison to null value")
        }
        val other = inputOtherObject as Comment
        if (other.id == null) {
            throw NullPointerException("Equal comparison against null id")
        }
        return this.id == other.id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}