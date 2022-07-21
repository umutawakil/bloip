package com.bloip.services

import com.bloip.domain.User
import com.bloip.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserService (
    @Autowired val userRepository: UserRepository,
    @Autowired val userCookieService: UserCookieService
)
{
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

    fun findAllUsersInDiscussion(discussionId: Long) : List<User> {
        return userRepository.findAllUsersInDiscussion(discussionId)
    }

    @Transactional
    fun resetCookies(user: User, code: String, ipAddress: String) {
        deleteCookies(
            userId = user.id!!
        )
        saveCookieInfo(
            user = user,
            code = code,
            ipAddress = ipAddress
        )
    }

    private fun deleteCookies(userId: Long) {
        userCookieService.deleteCookies(
            userId = userId
        )
    }

}