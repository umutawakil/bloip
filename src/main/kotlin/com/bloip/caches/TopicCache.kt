package com.bloip.caches

import com.bloip.domain.Topic
import com.bloip.repositories.TopicRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
@Component
class TopicCache(
    @Autowired val topicRepository: TopicRepository,
    @Autowired val loggingService: LoggingService
)
{
    private val topicsByFriendlyId: MutableMap<String, Topic> = mutableMapOf()
    private val topicsById: MutableMap<Long, Topic> = mutableMapOf()

    @PostConstruct
    fun init() {
        loggingService.log("\r\nLoading topic cache....")
        for(c in topicRepository.findAll()) {
            topicsByFriendlyId.put(c.friendlyId.lowercase(),c)
            topicsById.put(c.id, c)
        }
        loggingService.log("Loaded ${topicsByFriendlyId.size} topics\r\n")
    }

    fun getAll() : Collection<Topic> {
        return topicsByFriendlyId.values
    }

    fun get(topicId: Long) : Topic? {
        return topicsById[topicId]
    }
    fun get(friendlyId: String) : Topic? {
        return topicsByFriendlyId[friendlyId.lowercase()]
    }
}