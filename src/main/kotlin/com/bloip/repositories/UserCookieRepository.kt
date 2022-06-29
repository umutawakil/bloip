package com.bloip.repositories

import com.bloip.domain.UserCookie
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
interface UserCookieRepository: CrudRepository <UserCookie, Long?> {
    fun findByCode(code: String) : UserCookie?
}