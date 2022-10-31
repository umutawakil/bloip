package com.bloip.domain.inbox

import com.bloip.domain.discussion.DiscussionSubscriptionId
import java.io.Serializable
import javax.persistence.Embeddable

/**
 * Created by Usman Mutawakil on 10/30/22.
 */
@Embeddable
class InboxItemId : Serializable {
    val discussionId: Long
    val userId: Long

    constructor(discussionId: Long, userId: Long) {
        this.discussionId = discussionId
        this.userId       = userId
    }

    override fun equals(other: Any?): Boolean {
        val them = other as DiscussionSubscriptionId
        return (them.discussionId == this.discussionId) && (them.userId == this.userId)
    }

    override fun hashCode(): Int {
        return "$discussionId$userId".hashCode()
    }
}