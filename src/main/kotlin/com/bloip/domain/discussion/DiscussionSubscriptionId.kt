package com.bloip.domain.discussion

import java.io.Serializable
import javax.persistence.Embeddable

/**
 * Created by Usman Mutawakil on 8/31/22.
 */

@Embeddable
class DiscussionSubscriptionId : Serializable {
    val discussionId: Long
    val userId: Long

    constructor(discussionId: Long, userId: Long) {
        this.discussionId = discussionId
        this.userId       = userId
    }
}