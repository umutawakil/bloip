package com.bloip.domain

import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null

    @Column
    var title: String

    @Column
    var audioUrl: String? = null

    @Column
    var ipAddress: String

    @Column
    var numberOfReplies: Int = 0

    @Column
    val creationTimestamp: Date = Date()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    var user: User

   @OneToMany(fetch = FetchType.LAZY)
   @JoinColumn(name = "discussion_Id", referencedColumnName = "id")
   lateinit var comments: List<Comment>

    constructor(user: User, title: String, ipAddress: String) {
        this.user      = user
        this.title     = title
        this.ipAddress = ipAddress
    }

    fun getUrl(): String {
        return "/b/" + this.id
    }
}