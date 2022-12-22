package com.bloip.domain.user.authentication

import com.bloip.domain.value.EmailAddress
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
@Entity
@Table(name = "authentication_user_detail")
class AuthenticationUserDetail : UserDetails, StandardDomainObject {
    @JoinColumn(name="user_id", referencedColumnName = "id")
    @OneToOne(fetch = FetchType.EAGER)
    val user: User

    @Embedded
    private var username: EmailAddress

    @Column
    private var password: String

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name ="authentication_user_detail_role",
        joinColumns = [JoinColumn(name = "auth_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()

    @Version
    private val version = 0

    constructor(user: User, email: EmailAddress, password: String) {
        this.user     = user
        this.username = email
        this.password = password
    }

    fun setEmailAddress(emailAddress: EmailAddress) {
        this.username = emailAddress
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return this.roles
    }

    override fun getPassword(): String {
        return password
    }

    fun setPassword(x: String) {
        this.password = x
    }

    override fun getUsername(): String {
        return username.value
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}