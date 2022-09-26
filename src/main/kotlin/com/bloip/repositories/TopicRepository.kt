package com.bloip.repositories

import com.bloip.domain.Topic
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
interface TopicRepository : CrudRepository<Topic, Long> {
    @Query("SELECT t FROM Topic t ORDER BY t.id ASC")
    fun findAllByIdAscending(): List<Topic>
}