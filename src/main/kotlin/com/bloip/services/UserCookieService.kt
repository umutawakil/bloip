package com.bloip.services

import com.bloip.domain.User
import com.bloip.domain.UserCookie
import com.bloip.repositories.UserCookieRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserCookieService (@Autowired val userCookieRepository: UserCookieRepository){
    fun findByCode(code: String) : UserCookie? {
        return userCookieRepository.findByCode(code)
    }

    fun saveCookieInfo(user: User, code: String, ipAddress: String) {
        userCookieRepository.save(UserCookie(user, code, ipAddress))
    }

    fun deleteCookies(userId: Long) {
        userCookieRepository.deleteCookies(
            userId = userId
        )
    }
}