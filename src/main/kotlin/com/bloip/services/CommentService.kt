package com.bloip.services

import com.bloip.caches.CommentCache
import com.bloip.domain.Comment
import com.bloip.repositories.CommentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 9/7/22.
 */
@Service
class CommentService(
    @Autowired private val commentRepository: CommentRepository,
    @Autowired private val commentCache: CommentCache
) {
    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        return commentCache.getComments(discussionId = discussionId, start = start, end = end)
    }

    fun save(comment: Comment) : Comment {
        val commentUpdated =  commentRepository.save(comment)
        commentCache.save(commentUpdated)
        return commentUpdated
    }
}