package com.bloip.services

import com.bloip.caches.TopicCache
import com.bloip.domain.Topic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
@Service
class TopicService (
     @Autowired private val topicCache: TopicCache
)
{
    fun getAll() : Collection<Topic>{
        return topicCache.getAll()
    }

    fun get(topicId: Long) : Topic? {
        return topicCache.get(topicId = topicId)
    }
    fun get(friendlyId: String) : Topic? {
        return topicCache.get(friendlyId = friendlyId)
    }
}