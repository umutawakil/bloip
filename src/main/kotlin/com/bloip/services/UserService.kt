package com.bloip.services

import com.bloip.caches.UserCache
import com.bloip.domain.User
import com.bloip.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserService (
    @Autowired val userRepository: UserRepository,
    @Autowired val userCache: UserCache,
    @Autowired val userCookieService: UserCookieService
)
{
    fun createNewUser() : User {
        val user: User = userRepository.save(User())
        userCache.add(user)
        return user
    }

    fun findById(userId: Long) : User? {
        return userCache.findById(userId)
    }

    fun isNotActiveUser(userId: Long) : Boolean {
        return !isActiveUser(userId)
    }

    fun isActiveUser(userId: Long) : Boolean {
        return userCache.contains(userId)
    }

    fun findByCookieCode(code: String) : User? {
        return userCookieService.findByCode(code)?.getUser()
    }

    @Transactional
    fun resetCookies(user: User, code: String, ipAddress: String) {
        deleteCookies(
            userId = user.id
        )
        saveCookieInfo(
            user = user,
            code = code,
            ipAddress = ipAddress
        )
    }

    fun saveCookieInfo(user: User, code: String, ipAddress: String) {
        userCookieService.saveCookieInfo(user, code, ipAddress)
    }

    private fun deleteCookies(userId: Long) {
        userCookieService.deleteCookies(
            userId = userId
        )
    }

}