package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.LoggingService
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class DiscussionCache(
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
)
{
    //TODO: How do syncrhonized list and conccurrent hashmap work?
    private val discussions: BumpStack<Long, Discussion> = BumpStack()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing discussion cache")

        /** Cache each individual discussion with its comments greedily loaded **/
        val viewDiscussionResults: List<Discussion> = discussionRepository.findAllWithComments()
        for(d: Discussion in viewDiscussionResults) {
            discussions.push(d.id, d)
        }
        loggingService.log("Discussion cache initialized: ${discussions.size()} discussions loaded!\r\n\r\n")
    }

    fun getNextPage(offSetKey: Long?) : BumpStack.Page<Long, Discussion> {
        return discussions.nextPage(inputKey = offSetKey, applicationProperties.discussionsPerPage)
    }

    fun getPreviousPage(offSetKey: Long) : BumpStack.Page<Long, Discussion> {
        return discussions.previousPage(inputKey = offSetKey, applicationProperties.discussionsPerPage)
    }

    fun get(discussionId: Long): Discussion? {
        return discussions.get(discussionId)
    }

    fun push(discussion: Discussion) {
        discussions.push(discussion.id, discussion)
    }
}