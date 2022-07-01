package com.bloip.domain

import java.util.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

    @Column
    val title: String

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
    val user: User

   @OneToMany(fetch = FetchType.LAZY,  cascade = arrayOf(CascadeType.ALL))
   @JoinColumn(name = "discussion_Id", referencedColumnName = "id")
   lateinit var comments: MutableList<Comment>

    constructor(user: User, title: String, ipAddress: String) {
        this.user      = user
        this.title     = title
        this.ipAddress = ipAddress
    }

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/b/" + this.id
    }
}