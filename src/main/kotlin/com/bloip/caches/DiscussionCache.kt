package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.LoggingService
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
    private val discussionsByTime: MutableList<Discussion> = Collections.synchronizedList(LinkedList())
    private val discussions: MutableMap<Long, Discussion> = ConcurrentHashMap<Long, Discussion>()
    private val titles: MutableSet<String> = HashSet<String>()

    @PostConstruct
    fun init() {
        loggingService.log("\r\n\r\nInitializing discussion cache")

        /** Cache each individual discussion with its comments greedily loaded **/
        val viewDiscussionResults: List<Discussion> = discussionRepository.findAllWithComments()
        for(d: Discussion in viewDiscussionResults) {
            discussions[d.id] = d
            discussionsByTime.add(d)
            titles.add(d.title.lowercase())
        }

        loggingService.log("Discussion cache initialized.\r\n\r\n")
    }

    fun getPage(inputPageNumber: Int?) : List<Discussion> {
        var pageNumber = 0
        if (inputPageNumber != null) {
            pageNumber = inputPageNumber
        }

        val offset: Int = pageNumber * applicationProperties.discussionsPerPage
        val end: Int = if(offset + applicationProperties.discussionsPerPage > discussionsByTime.size)
            discussionsByTime.size
        else
            offset + applicationProperties.discussionsPerPage

        return discussionsByTime.subList(
            fromIndex = offset,
            toIndex = end
        )
    }

    fun getWithComments(discussionId: Long): Discussion? {
        return discussions[discussionId]
    }

    fun hasTitle(title: String): Boolean {
        return titles.contains(title.lowercase())
    }

    /**TODO: This probably needs some sort of in memory transaction **/
    fun add(discussion: Discussion) {
        discussionsByTime.add(
            index = 0,
            element = discussion
        )
        discussions.put(discussion.id, discussion)
        titles.add(discussion.title)
    }
}