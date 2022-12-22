package com.bloip.repositories

import com.bloip.domain.user.authentication.Role
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/24/22.
 */
interface RoleRepository : CrudRepository<Role, Long> {
    fun findByAuthority(authority: String) : List<Role>
}