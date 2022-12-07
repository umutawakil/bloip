package com.bloip.utilities

import com.bloip.domain.authentication.AdminRoles
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import javax.servlet.http.HttpServletRequest


/**
 * Created by Usman Mutawakil on 11/20/22.
 */
class AuthUtility {

    companion object {
        fun isSamurai() : Boolean {
            try {
                return isSamuraiHelper()
            } catch (exception: Exception) {
                exception.printStackTrace() //TODO: temporary till more advanced security stuff comes about
            }
            return false
        }
        fun isSamuraiHelper() : Boolean {
            val principal = SecurityContextHolder.getContext().authentication.principal
            if (principal !is UserDetails) {
                return false
            }
            for(a: GrantedAuthority in principal.authorities) {
                for(r in AdminRoles.values()) {
                    if(a.authority.lowercase() == r.name.lowercase()) {
                        return true
                    }
                }
            }
            return false
        }
    }
}