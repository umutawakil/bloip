package com.bloip.services

import com.bloip.caches.UserCookieCache
import com.bloip.domain.User
import com.bloip.domain.UserCookie
import com.bloip.repositories.UserCookieRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class UserCookieService (
    @Autowired val userCookieCache: UserCookieCache,
    @Autowired val userCookieRepository: UserCookieRepository
    ){
    fun findByCode(code: String) : UserCookie? {
        return userCookieCache.findByCode(code)
    }

    fun saveCookieInfo(user: User, code: String, ipAddress: String) {
        val newUserCookie = userCookieRepository.save(UserCookie(user, code, ipAddress))

        userCookieCache.saveCookieInfo(
            user = user,
            newUserCookie  = newUserCookie
        )
    }

    fun deleteCookies(userId: Long) {
        userCookieRepository.deleteCookies(
            userId = userId
        )

        userCookieCache.deleteCookies(userId)
    }
}