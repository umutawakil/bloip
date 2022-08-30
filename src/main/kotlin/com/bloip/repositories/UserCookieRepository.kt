package com.bloip.repositories

import com.bloip.domain.UserCookie
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
interface UserCookieRepository: CrudRepository <UserCookie, Long?> {
    @Query("DELETE FROM UserCookie uc WHERE uc.user.id = ?1")
    @Modifying
    fun deleteCookies(userId: Long)
}