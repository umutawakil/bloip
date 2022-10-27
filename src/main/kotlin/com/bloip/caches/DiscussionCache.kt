package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.LoggingService
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class DiscussionCache(
    @Autowired private val discussionRepository: DiscussionRepository,
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService
)
{
    private val discussions: MutableMap<Long, Discussion> = ConcurrentHashMap<Long, Discussion>()
    private val allDiscussionsSorted: BumpStack<Long, Discussion> = BumpStack()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing discussion cache")

        /** Cache each individual discussion with its comments greedily loaded **/
        val discussionResults: List<Discussion> = discussionRepository.findAllAscending()
        for(d: Discussion in discussionResults) {
            this.push(discussion = d)
        }

        loggingService.log(
            "Discussion cache initialized: ${discussions.size} discussions loaded across " +
                    "and sorted in a bumpstack of all ${allDiscussionsSorted.size()} discussions!\r\n\r\n"
        )
    }

    fun getNextPage(offSetKey: Long?): BumpStack.Page<Long, Discussion> {
        return allDiscussionsSorted.nextPage(
            inputKey = offSetKey,
            N = applicationProperties.discussionsPerPage
        )
    }

    fun getPreviousPage(offSetKey: Long) : BumpStack.Page<Long, Discussion> {
        return allDiscussionsSorted.previousPage(
            inputKey = offSetKey,
                   N = applicationProperties.discussionsPerPage
        )
    }

    fun get(discussionId: Long): Discussion? {
        return discussions[discussionId]
    }

    fun push(discussion: Discussion) {
        discussions[discussion.id] = discussion
        allDiscussionsSorted.push(key = discussion.id, discussion)
    }

    fun bump(discussionId: Long) {
        allDiscussionsSorted.bump(key = discussionId)
    }

    fun getSize() : Int {
        return allDiscussionsSorted.size()
    }

    fun update(discussion: Discussion) {
        discussions[discussion.id] = discussion
        allDiscussionsSorted.update(key = discussion.id, value = discussion)
    }
}