package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 8/31/22.
 */
@Entity
@Table(name = "discussion_subscription")
class DiscussionSubscription {
    @EmbeddedId
    val id: DiscussionSubscriptionId

    constructor(id: DiscussionSubscriptionId) {
        this.id = id
    }
}