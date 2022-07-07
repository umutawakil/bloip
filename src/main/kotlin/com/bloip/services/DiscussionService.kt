package com.bloip.services

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Comment
import com.bloip.domain.Discussion
import com.bloip.domain.User
import com.bloip.repositories.DiscussionRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(
        @Autowired val commentService: CommentService,
        @Autowired val discussionRepository: DiscussionRepository,
        @Autowired val userService: UserService,
        @Autowired val notificationService: NotificationService,
        @Autowired val loggingService: LoggingService,
        @Autowired val applicationProperties: ApplicationProperties
    ) {
    fun getPage(inputPageNumber: Int?) : Page<Discussion> {
        var pageNumber: Int = 0
        if (inputPageNumber != null) {
            pageNumber = inputPageNumber
        }

        val pageRequest: Pageable = PageRequest.of(pageNumber, applicationProperties.discussionsPerPage, Sort.by("id").descending())
        return discussionRepository.findAll(pageRequest)
    }

    fun getWithComments(discussionId: Long): Discussion? {
        val result: List<Discussion> = discussionRepository.findWithComments(discussionId)
        if(result.isNotEmpty()) {

            result[0].comments = result[0].comments.sortedBy { it.id }.toMutableList()
            return result[0]
        }
        return null
    }

    fun findByTitle(title: String): Discussion? {
        return discussionRepository.findByTitleIgnoreCase(title)
    }

    @Transactional
    fun create(userId: Long, title: String, ipAddress: String): Discussion {
        val user: User = userService.findById(userId)!!
        var discussion = discussionRepository.save(
            Discussion(
                user = user,
                title = title,
                ipAddress = ipAddress
            )
        )

        val comment = Comment(
            user = user,
            discussion = discussion,
            ipAddress = ipAddress
        )
        commentService.save(comment)

        return discussion
    }

    fun findById(id: Long) : Discussion? {
        return discussionRepository.findById(id).get()
    }

    fun save(discussion: Discussion): Discussion {
        return discussionRepository.save(discussion)
    }

    @Transactional
    fun reply(userId: Long, discussionId: Long, ipAddress: String): Discussion {
        val discussion: Discussion = findById(discussionId)!!
        discussion.numberOfReplies++

        notificationService.notifyAll(senderId = userId, discussionId = discussionId)

        val user: User = userService.findById(userId)!!
        val comment = Comment(
            user = user,
            discussion = discussion,
            ipAddress = ipAddress
        )

        discussion.comments.add(comment)
        return save(discussion)
    }

}