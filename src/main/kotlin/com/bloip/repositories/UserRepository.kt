package com.bloip.repositories

import com.bloip.domain.User
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
interface UserRepository : PagingAndSortingRepository<User, Long> {
    @Query("SELECT u FROM User u JOIN Comment c ON c.user.id = u.id JOIN Discussion d ON d.id = c.discussion.id WHERE d.id = ?1")
    fun findAllUsersInDiscussion(discussionId: Long) : List<User>
}