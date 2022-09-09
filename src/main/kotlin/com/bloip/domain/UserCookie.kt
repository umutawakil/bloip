package com.bloip.domain

import javax.persistence.*

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Entity
@Table(name = "user_cookie")
class UserCookie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private val id: Long? = null

    @Column
    private var code: String

    @Column
    private var ipAddress: String

    @ManyToOne(optional = true)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = true)
    private val user: User

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

    override fun equals(inputOtherObject: Any?) : Boolean {
        if (inputOtherObject == null) {
            throw NullPointerException("Equal comparison to null value")
        }
        val other = inputOtherObject as UserCookie
        if (other.id == null) {
            throw NullPointerException("Equal comparison against null id")
        }
        return this.id == other.id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}