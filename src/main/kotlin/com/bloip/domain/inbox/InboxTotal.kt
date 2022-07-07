package com.bloip.domain.inbox

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 7/6/22.
 */
@Entity
@Table(name = "v_inbox_total")
class InboxTotal {
    @Id
     val userId: Long

    @Column(name = "total")
    val total: Int

    constructor(userId: Long, total: Int) {
        this.userId = userId
        this.total  = total
    }
}