package com.bloip.caches

import com.bloip.domain.User
import com.bloip.domain.UserCookie
import com.bloip.repositories.UserCookieRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class UserCookieCache(
    @Autowired val userCookieRepository: UserCookieRepository,
    @Autowired val loggingService: LoggingService
) {
    private val userCookiesByUser: MutableMap<Long, MutableList<UserCookie>> = ConcurrentHashMap<Long, MutableList<UserCookie>>()
    private val userCookiesByCode: MutableMap<String, UserCookie> = ConcurrentHashMap<String, UserCookie>()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing user cookie cache")

        for(uc: UserCookie in userCookieRepository.findAll()) {
            userCookiesByCode[uc.getCode()] = uc

            var mutableList: MutableList<UserCookie>? = userCookiesByUser[uc.getUser().id]
            if(mutableList == null) {
                mutableList = ArrayList()
                userCookiesByUser[uc.getUser().id] = mutableList
            }
            mutableList.add(uc)
        }
        loggingService.log("UserCookie cache initialized\r\n\r\n")
    }

    fun findByCode(code: String) : UserCookie? {
        return userCookiesByCode[code]
    }

    fun saveCookieInfo(user: User, newUserCookie: UserCookie) {
        userCookiesByCode.put(newUserCookie.getCode(), newUserCookie)

        var usersCookies: MutableList<UserCookie>? = userCookiesByUser[user.id]
        if(usersCookies == null) {
            usersCookies = ArrayList()
            userCookiesByUser[user.id] = usersCookies
        }
        usersCookies.add(newUserCookie)
    }

    //TODO: This will need to change for multi-device accounts
    fun deleteCookies(userId: Long) {
        val userCookies: List<UserCookie>? = userCookiesByUser[userId]
        if(userCookies == null) {
            return
        }

        for(uc: UserCookie in userCookies) {
            userCookiesByCode.remove(uc.getCode())
        }
        userCookiesByUser.remove(userId)
    }
}