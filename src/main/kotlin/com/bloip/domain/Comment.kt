package com.bloip.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.util.Date
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
    var audioUrl: String? = null

    @Column
    val creationTimestamp: Date = java.util.Date()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", referencedColumnName = "id")
    @JsonIgnore
    val discussion: Discussion

    @Column
    var ipAddress:String

    constructor(user: User, discussion: Discussion, ipAddress: String) {
        this.user       = user
        this.discussion = discussion
        this.ipAddress  = ipAddress
    }
}