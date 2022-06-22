package com.bloip.domain

import java.sql.Date
import javax.persistence.*

@Entity
@Table(name = "discussion")
 class Discussion {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @Column(name = "id")
    var id: Long? = null

    @Column
    lateinit var title: String

    @Column
    lateinit var audioUrl:String

    @Column
    lateinit var url: String

    @Column
    var numberOfReplies: Int = 0

    @Column
    lateinit var creationTimestamp: Date

    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var user: User
}