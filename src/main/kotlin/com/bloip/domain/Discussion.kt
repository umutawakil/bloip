package com.bloip.domain

import java.util.Date
import java.util.stream.Collectors
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0

    @Column
    val title: String

    @Column
    val audioUrl: String? = "https://www.w3schools.com/html/horse.mp3"//null

    @Column
    val ipAddress: String

    @Column
    var numberOfReplies: Int = 0

    @Column
    val creationTimestamp: Date = Date()

    @Column
    val updateTimestamp: Date = Date()

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    val user: User

   @OneToMany(fetch = FetchType.EAGER,  cascade = arrayOf(CascadeType.ALL))
   @JoinColumn(name = "discussion_Id", referencedColumnName = "id")
   val comments: MutableList<Comment> = mutableListOf()
   get() {
       return field!!.stream().sorted { o1, o2 -> o1.creationTimestamp.compareTo(o2.creationTimestamp)  }.collect(
           Collectors.toList())
   }

    constructor(user: User, title: String, ipAddress: String) {
        this.user      = user
        this.title     = title
        this.ipAddress = ipAddress
    }

    /** This is used dynamically in a .html template. Ignore the gray (nousages) **/
    fun getUrl(): String {
        return "/d/" + this.id
    }
}