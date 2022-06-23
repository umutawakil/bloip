package com.bloip.services

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.repositories.DiscussionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(@Autowired val discussionRepository: DiscussionRepository, @Autowired val applicationProperties: ApplicationProperties) {
    fun getPage(inputPageNumber: Int?) : Page<Discussion> {
        var pageNumber: Int = 0
        if (inputPageNumber != null) {
            pageNumber = inputPageNumber
        }
        val pageRequest: Pageable = PageRequest.of(pageNumber, applicationProperties.discussionsPerPage, Sort.by("id").descending())
        return discussionRepository.findAll(pageRequest)
    }

    fun getWithComments(discussionId: Int): Discussion? {
        val result: List<Discussion> = discussionRepository.findWithComments(discussionId)
        if(result.isNotEmpty()) {

            println("Number of comments: " + result[0].comments.size)
            result[0].comments = result[0].comments.sortedBy { it.id }
            return result[0]
        }
        return null
    }
}