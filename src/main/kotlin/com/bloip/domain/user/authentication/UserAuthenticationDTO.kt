package com.bloip.domain.user.authentication

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
open class UserAuthenticationDTO : UserDetails {
    private var username: String
    private var password: String
    val roles: MutableSet<Role>


    constructor(username: String, password: String, roles: MutableSet<Role>) {
        this.username = username
        this.password = password
        this.roles    = roles
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return this.roles
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
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