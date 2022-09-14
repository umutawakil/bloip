package com.bloip.repositories

import com.bloip.domain.Topic
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
interface TopicRepository : CrudRepository<Topic, Long>