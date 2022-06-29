package com.bloip.services

import com.bloip.domain.User
import com.bloip.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserService (@Autowired val userRepository: UserRepository, @Autowired val userCookieService: UserCookieService) {
    fun createNewUser() : User {
        return userRepository.save(User())
    }

    fun findByCookieCode(code: String) : User? {
        return userCookieService.findByCode(code)?.getUser()
    }

    fun saveCookieInfo(user: User, code: String, ipAddress: String) {
        userCookieService.saveCookieInfo(user, code, ipAddress)
    }

    fun findById(userId: Long) : User? {
        return userRepository.findById(userId).get()
    }
}