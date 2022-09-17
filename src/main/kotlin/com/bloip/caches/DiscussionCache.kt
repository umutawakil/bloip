package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.LoggingService
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 8/11/22.
 */
@Component
class DiscussionCache(
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val topicCache: TopicCache,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
)
{
    private val discussions: MutableMap<Long, Discussion> = mutableMapOf()
    private val discussionsByTopic: MutableMap<String, BumpStack<Long, Discussion>> = mutableMapOf()
    private val allDiscussionsSorted: BumpStack<Long, Discussion> = BumpStack()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing discussion cache")

        /** Initialize each topic index with an  empty bump stack. This is also helpful in integration testing. **/
        for (t in topicCache.getAll()) {
            discussionsByTopic[t.friendlyId.lowercase()] = BumpStack()
        }

        /** Cache each individual discussion with its comments greedily loaded **/
        val discussionResults: List<Discussion> = discussionRepository.findAllAscending()
        for(d: Discussion in discussionResults) {
            this.push(discussion = d)
        }

        loggingService.log(
            "Discussion cache initialized: ${discussions.size} discussions loaded across " +
                    "${discussionsByTopic.size} categories and sorted in a bumpstack of all ${allDiscussionsSorted.size()} discussions!\r\n\r\n"
        )
    }

    fun getNextPage(topicFriendlyId: String?, offSetKey: Long?) : BumpStack.Page<Long, Discussion> {
        if (topicFriendlyId == null) {
            /**return allDiscussionsSorted.nextPage(
                inputKey = offSetKey,
                N        = applicationProperties.discussionsPerPage
            )**/
            val bump =  allDiscussionsSorted.nextPage(
                inputKey = offSetKey,
                N        = applicationProperties.discussionsPerPage
            )
            return bump
        }

        return discussionsByTopic[topicFriendlyId.lowercase()]!!.
        nextPage(
            inputKey = offSetKey,
            N        = applicationProperties.discussionsPerPage
        )
    }

    fun getPreviousPage(topicFriendlyId: String?, offSetKey: Long) : BumpStack.Page<Long, Discussion> {
        if (topicFriendlyId == null) {
            return allDiscussionsSorted.previousPage(
                inputKey = offSetKey,
                       N = applicationProperties.discussionsPerPage
            )
        }

        return discussionsByTopic[topicFriendlyId.lowercase()]!!.
        previousPage(
            inputKey = offSetKey,
                   N = applicationProperties.discussionsPerPage
        )
    }

    fun get(discussionId: Long): Discussion? {
        return discussions[discussionId]
    }

    fun push(discussion: Discussion) {
        discussions[discussion.id] = discussion

        discussionsByTopic[discussion.topic.friendlyId.lowercase()]!!.push(discussion.id, discussion)

        allDiscussionsSorted.push(key = discussion.id, discussion)

        topicCache.get(friendlyId = discussion.topic.friendlyId.lowercase())!!.count++
    }

    fun bump(discussionId: Long) {
        val discussion: Discussion = discussions[discussionId] ?: return

        discussionsByTopic[discussion.topic.friendlyId.lowercase()]!!.
            bump(key = discussionId)

        allDiscussionsSorted.bump(key = discussionId)
    }
}