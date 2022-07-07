package com.bloip.domain.inbox

import com.bloip.domain.Discussion
import com.bloip.domain.User
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Entity
@Table(name = "v_inbox")
class InboxItem {
    @EmbeddedId
    val inboxItemId : InboxItemId

    @Column(name = "count")
    val count:Int

    @Column(name = "title")
    val title: String

    constructor(inboxItemId: InboxItemId, count: Int, title: String) {
        this.inboxItemId = inboxItemId
        this.count       = count
        this.title       = title
    }
}