package com.bloip.repositories

import com.bloip.domain.Discussion
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.PagingAndSortingRepository

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
interface DiscussionRepository : PagingAndSortingRepository<Discussion, Long> {
    @Query("SELECT d FROM Discussion d ORDER BY d.updateTimestamp ASC")
    fun findAllAscending(): List<Discussion>
}