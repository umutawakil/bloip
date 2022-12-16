package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.localization.Country
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
    private val allDiscussionsSorted: MutableMap<Country, BumpStack<Long, Discussion>> = ConcurrentHashMap()

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
                    "and sorted in ${allDiscussionsSorted.size} stacks (1 for each country represented)\r\n\r\n"
        )
    }

    fun getNextPage(country: Country, offSetKey: Long?): BumpStack.Page<Long, Discussion> {
        val result = getStackByCountry(country = country)?.nextPage(
            inputKey = offSetKey,
            N = applicationProperties.discussionsPerPage
        )

        return result ?: BumpStack.Page(null, null, listOf())
    }

    fun getPreviousPage(country: Country, offSetKey: Long) : BumpStack.Page<Long, Discussion> {
        val result =  getStackByCountry(country = country)?.previousPage(
            inputKey = offSetKey,
                   N = applicationProperties.discussionsPerPage
        )
        return result ?: BumpStack.Page(null, null, listOf())
    }

    fun get(discussionId: Long): Discussion? {
        return discussions[discussionId]
    }

    fun push(discussion: Discussion) {
        discussions[discussion.id] = discussion

        val bumpStack: BumpStack<Long, Discussion> = allDiscussionsSorted[discussion.country] ?: BumpStack()
        bumpStack.push(key = discussion.id, discussion)
        if (!allDiscussionsSorted.contains(discussion.country)) {
            allDiscussionsSorted[discussion.country] = bumpStack
        }
    }

    fun bump(discussionId: Long) {
        val discussion: Discussion = discussions[discussionId] ?: return
        getStackForDiscussionByCountry(discussion)?.bump(key = discussionId)
    }

    fun getSize() : Int {
        return discussions.size
    }

    fun update(discussion: Discussion) {
        discussions[discussion.id] = discussion

        val stack: BumpStack<Long, Discussion> = allDiscussionsSorted[discussion.country] ?: BumpStack()
        if (!allDiscussionsSorted.contains(discussion.country)) {
            allDiscussionsSorted[discussion.country] = stack
        }
        stack.update(key = discussion.id, value = discussion)
    }

    fun getStackForDiscussionByCountry(discussion: Discussion) : BumpStack<Long, Discussion>? {
        return allDiscussionsSorted[discussion.country]
    }

    fun getStackByCountry(country: Country) : BumpStack<Long, Discussion>? {
        return allDiscussionsSorted[country]
    }

    fun deleteAll() {
        allDiscussionsSorted.clear()
        discussions.clear()
    }
}