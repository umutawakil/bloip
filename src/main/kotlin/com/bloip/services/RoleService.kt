package com.bloip.services

import com.bloip.domain.authentication.AdminRoles
import com.bloip.domain.authentication.Role
import com.bloip.repositories.RoleRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/24/22.
 */
@Service
class RoleService(
    @Autowired val roleRepository: RoleRepository
)
{
    private val roles: MutableSet<Role> = mutableSetOf()

    @PostConstruct
    fun init() {
        for(r: Role in roleRepository.findAll()) {
            roles.add(r)
        }
        if (roles.size == 0) {
            for(x in AdminRoles.values()) {
                roleRepository.save(Role(authority = x.name))
            }
        }
    }

    fun getRoles() : Set<Role> {
        return this.roles
    }
}