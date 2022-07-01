package com.bloip.services

import com.bloip.domain.Comment
import com.bloip.repositories.CommentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/30/22.
 */
@Service
class CommentService(
    @Autowired val commentRepository : CommentRepository
    )
{
    fun save(comment: Comment) : Comment {
        return this.commentRepository.save(comment)
    }
}