package com.bloip.domain.discussion

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
@Entity
@Table(name = "discussion_subscription")
class DiscussionSubscription : StandardDomainObject {
    /*@EmbeddedId
    val id: DiscussionSubscriptionId

    @Version
    private val version = 0

    constructor(id: DiscussionSubscriptionId) {
        this.id = id
    }*/
    @Column(name="discussion_id")
    val discussionId: Long
    @Column(name="user_id")
    val userId: Long

    constructor(discussionId: Long, userId: Long) {
        this.discussionId = discussionId
        this.userId       = userId
    }

    override fun equals(other: Any?): Boolean {
        val them = other as DiscussionSubscription
        return (them.discussionId == this.discussionId) && (them.userId == this.userId)
    }

    override fun hashCode(): Int {
        return "$discussionId$userId".hashCode()
    }
}