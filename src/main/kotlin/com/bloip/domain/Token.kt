package com.bloip.domain

import java.util.*
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 12/5/22.
 */
@Entity
@Table(name = "token")
class Token : StandardDomainObject {
    @JoinColumn(name="user_id", referencedColumnName = "id")
    @OneToOne(fetch = FetchType.EAGER)
    val user: User?
    @Column
    val email: String
    @Column
    val value: String
    @Column
    val creationTimestamp: Date

    constructor(user: User?, email: String) {
        this.user              = user
        this.email             = email
        this.value             = UUID.randomUUID().toString()
        this.creationTimestamp = Date()
    }
}