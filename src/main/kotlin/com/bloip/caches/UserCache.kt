package com.bloip.caches

import com.bloip.domain.User
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

    @PostConstruct
    fun init() {
        loggingService.log("Initializing user cache")

        for(u:User in userRepository.findAll()) {
            users[u.id] = u
        }
        loggingService.log("User cache initialized\r\n\r\n")
    }

    fun findById(userId: Long) : User? {
        return users[userId]
    }

    fun add(user: User) {
        users[user.id] = user
    }

    fun contains(userId: Long) : Boolean {
        return this.users.containsKey(userId)
    }
}