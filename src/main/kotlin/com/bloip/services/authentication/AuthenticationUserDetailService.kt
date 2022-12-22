package com.bloip.services.authentication

import com.bloip.domain.user.authentication.AuthenticationUserDetail
import com.bloip.repositories.UserDetailRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import kotlin.jvm.Throws

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
//@Service("userDetailService")
class AuthenticationUserDetailService(
    @Autowired private val authenticationUserDetailRepository: UserDetailRepository
) {

    private val authenticationDetailsByUserName: MutableMap<String, AuthenticationUserDetail> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        for(userDetail in authenticationUserDetailRepository.findAll()) {
            authenticationDetailsByUserName[userDetail.username] = userDetail
        }
    }

    @Throws(UsernameNotFoundException::class)
    fun loadUserByUsername(username: String?): AuthenticationUserDetail {
        return authenticationDetailsByUserName[username] ?: throw UsernameNotFoundException("Username not found")
    }

    fun usernameExists(username: String) : Boolean {
       return authenticationDetailsByUserName[username] != null
    }

    //TODO: Needs work. Seems to pass when it should fail
    /*fun login(req: HttpServletRequest, username: String, password: String) : Boolean {
        println("PASSWORD: " + password)
        val authReq = UsernamePasswordAuthenticationToken(username, password)
        val auth: Authentication = authenticationManager.authenticate(authReq)
        val sc = SecurityContextHolder.getContext()
        sc.authentication = auth
        val session = req.getSession(true)
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc)

        return auth.isAuthenticated
    }*/

    fun save(authenticationUserDetail: AuthenticationUserDetail) : AuthenticationUserDetail {
        authenticationDetailsByUserName[
                authenticationUserDetail.username
        ] = authenticationUserDetailRepository.save(authenticationUserDetail)

        return authenticationDetailsByUserName[authenticationUserDetail.username]!!
    }

    fun delete(username: String) {
        authenticationDetailsByUserName.remove(key = username)
    }
}