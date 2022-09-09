package com.bloip.domain

import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0

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

    constructor(userId: Long, title: String, ipAddress: String) {
        this.userId            = userId
        this.title             = title
        this.ipAddress         = ipAddress
        this.creationTimestamp = Date()
        this.updateTimestamp   = Date()
    }

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/d/" + this.id
    }

    override fun equals(inputOtherObject: Any?) : Boolean {
        if (inputOtherObject == null) {
            throw NullPointerException("Equal comparison to null value")
        }
        val other = inputOtherObject as Discussion
        if (other.id == null) {
            throw NullPointerException("Equal comparison against null id")
        }

        return this.id == other.id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}