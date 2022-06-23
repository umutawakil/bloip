package com.bloip.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import java.sql.Date
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Entity
@Table(name = "comment")
class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator="native")
    @Column(name = "id")
    var id: Int? = null

    @Column
    lateinit var audioUrl:String

    @Column
    lateinit var creationTimestamp: Date

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    lateinit var user: User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", referencedColumnName = "id")
    @JsonIgnore
    lateinit var discussion: Discussion
}