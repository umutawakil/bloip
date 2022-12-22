package com.bloip.domain.user.authentication

import com.bloip.domain.StandardDomainObject
import org.springframework.security.core.GrantedAuthority
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 11/24/22.
 */
@Entity
@Table(name="role")
class Role : GrantedAuthority, StandardDomainObject {
    @Column
    private val authority: String

    constructor(authority: String) {
        this.authority = authority
    }

    override fun getAuthority(): String {
        return this.authority
    }
}