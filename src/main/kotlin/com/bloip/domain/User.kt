package com.bloip.domain

import java.util.Date
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/21/22.
 */
@Entity
@Table(name = "user")
class User : StandardDomainObject
{
    companion object {
        const val DISCUSSION_CREATION_LIMIT = 3
    }

    constructor()

    /** Code below is a temporary quick fix for limiting users to 10 discussions a day **/
    @Transient
    private var discussionsCreatedInLastDay: Int = 0

    @Transient
    private var firstCreationDate: Date? = null

    fun recordNewDiscussion() {
        if(firstCreationDate == null) {
            firstCreationDate = Date(System.currentTimeMillis())
        }
        discussionsCreatedInLastDay++
    }

    fun discussionCreationLimitReached() : Boolean {
        if (firstCreationDate == null) return false

        if (discussionsCreatedInLastDay < DISCUSSION_CREATION_LIMIT) {
            return false
        }

        val DAY_IN_MILLIS = 3600*24*1000L
        val finalDate     = Date(this.firstCreationDate!!.time + DAY_IN_MILLIS)
        val currentDate   = Date(System.currentTimeMillis())

        if (currentDate.after(finalDate)) {
            discussionsCreatedInLastDay = 0
            return false
        }
        return true
    }
}