package com.bloip.domain

import java.sql.Date
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Entity
@Table(name = "user_cookie")
class UserCookie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY, )
    @Column(name = "id")
    private var id: Long? = null

    @Column
    private var code: String

    @Column
    private var creationTimestamp: Date? = Date(System.currentTimeMillis())

    @Column
    private lateinit var ipAddress: String

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private var user: User? = null

    fun getUser(): User? {
        return this.user
    }

    constructor(user: User, code: String, ipAddress: String) {
        this.user      = user
        this.code      = code
        this.ipAddress = ipAddress
    }

}