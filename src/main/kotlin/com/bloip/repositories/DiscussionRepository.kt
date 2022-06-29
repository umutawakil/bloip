package com.bloip.repositories

import com.bloip.domain.Discussion
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
interface DiscussionRepository : PagingAndSortingRepository<Discussion, Long> {
    @Query("SELECT d FROM Discussion d LEFT JOIN Fetch Comment c On c.discussion.id = d.id WHERE d.id = ?1")
    fun findWithComments(discussionId: Long): List<Discussion>

    fun findByTitleIgnoreCase(title: String): Discussion?
}