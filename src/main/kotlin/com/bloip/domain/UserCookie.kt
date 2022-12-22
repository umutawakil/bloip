package com.bloip.domain

import com.bloip.domain.user.User
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Entity
@Table(name = "user_cookie")
class UserCookie : StandardDomainObject{
    @Column
    private var code: String

    @Column
    private var ipAddress: String

    @ManyToOne(optional = true, cascade = [])
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private val user: User

    @Version
    private val version = 0

    fun getUser(): User {
        return this.user
    }

    fun getCode(): String {
        return this.code
    }

    constructor(user: User, code: String, ipAddress: String) {
        this.user      = user
        this.code      = code
        this.ipAddress = ipAddress
    }
}