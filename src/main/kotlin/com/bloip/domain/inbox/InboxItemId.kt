package com.bloip.domain.inbox

import com.bloip.domain.Discussion
import com.bloip.domain.User
import java.io.Serializable
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */

@Embeddable
class InboxItemId : Serializable {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_Id", referencedColumnName = "id")
    val user: User

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discussion_id", referencedColumnName = "id")
    val discussion: Discussion

    constructor(user: User, discussion: Discussion) {
        this.user = user
        this.discussion = discussion
    }
}