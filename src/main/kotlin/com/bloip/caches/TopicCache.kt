package com.bloip.caches

import com.bloip.domain.Topic
import com.bloip.repositories.TopicRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
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
    private val topicsByFriendlyId: MutableMap<String, Topic> = ConcurrentHashMap()
    private val topicsById: MutableMap<Long, Topic> = ConcurrentHashMap()
    private val allTopics: MutableList<Topic> = mutableListOf()

    @PostConstruct
    fun init() {
        loggingService.log("\r\nLoading topic cache....")
        for(c in topicRepository.findAllByIdAscending()) {
            topicsByFriendlyId.put(c.friendlyId.lowercase(),c)
            topicsById.put(c.id, c)
            allTopics.add(c)
        }
        loggingService.log("Loaded ${topicsByFriendlyId.size} topics\r\n")

    }

    fun getAll() : List<Topic> {
        return allTopics
    }

    fun get(topicId: Long) : Topic? {
        return topicsById[topicId]
    }
    fun get(friendlyId: String) : Topic? {
        return topicsByFriendlyId[friendlyId.lowercase()]
    }
}