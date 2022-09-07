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
    @Autowired val commentRepository: CommentRepository,
    @Autowired val commentCache: CommentCache
) {
    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        return commentCache.getComments(discussionId = discussionId, start = start, end = end)
    }

    fun save(comment: Comment) : Comment{
        commentCache.save(comment)
        return commentRepository.save(comment)
    }
}