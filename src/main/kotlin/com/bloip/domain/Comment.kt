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
    var id: Int? = null

    @Column
    lateinit var audioUrl:String

    @Column
    val creationTimestamp: Date = java.util.Date()

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", referencedColumnName = "id")
    @JsonIgnore
    lateinit var discussion: Discussion
}