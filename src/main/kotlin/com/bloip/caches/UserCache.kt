package com.bloip.caches

import com.bloip.domain.User
import com.bloip.domain.authentication.AuthenticationUserDetail
import com.bloip.repositories.UserRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class UserCache(
    @Autowired val userRepository: UserRepository,
    @Autowired val loggingService: LoggingService
) {
    private val users: MutableMap<Long, User> = ConcurrentHashMap<Long, User>()
    private val authenticationDetailsByUserName: MutableMap<String, AuthenticationUserDetail> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing user cache")
        for(u:User in userRepository.findAll()) {
            users[u.id] = u
            if (u.authenticationUserDetail != null) {
                authenticationDetailsByUserName[u.authenticationUserDetail!!.username] = u.authenticationUserDetail!!
            }
        }
        loggingService.log("User cache initialized\r\n\r\n")
    }

    fun findById(userId: Long) : User? {
        return users[userId]
    }

    fun findByUserName(username: String) : User? {
        return authenticationDetailsByUserName[username]?.user
    }

    fun loadByUserName(username: String) : AuthenticationUserDetail? {
        return authenticationDetailsByUserName[username]?.user?.authenticationUserDetail
    }

    fun usernameExists(username: String) : Boolean {
        return authenticationDetailsByUserName[username] != null
    }

    fun add(user: User) {
        users[user.id] = user
        if (user.authenticationUserDetail != null) {
            authenticationDetailsByUserName[user.authenticationUserDetail!!.username] = user.authenticationUserDetail!!
        }
    }

    fun contains(userId: Long) : Boolean {
        return this.users.containsKey(userId)
    }

    fun delete(user: User) {
        users.remove(user.id)
        if (user.authenticationUserDetail != null) {
            authenticationDetailsByUserName.remove(user.authenticationUserDetail!!.username)
        }
    }

    fun purgeEmail(email: String) {
        authenticationDetailsByUserName.remove(email)
    }
}