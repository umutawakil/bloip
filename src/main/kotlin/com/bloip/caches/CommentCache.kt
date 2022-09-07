package com.bloip.caches

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Comment
import com.bloip.repositories.CommentRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/7/22.
 */
@Component
class CommentCache(
    @Autowired val commentRepository: CommentRepository,
    @Autowired val loggingService: LoggingService
)
{
    private val commentsByDiscussion: MutableMap<Long, MutableList<Comment>> = ConcurrentHashMap<Long, MutableList<Comment>>()

    @PostConstruct
    fun init() {
        loggingService.log("Loading comments cache")
        val tempComments: List<Comment> = commentRepository.findAll().toList()
        for (c: Comment in tempComments) {
            var discussionComments: MutableList<Comment>? = commentsByDiscussion[c.discussionId]
            if (discussionComments == null) {
                discussionComments = mutableListOf()
                commentsByDiscussion[c.discussionId] = discussionComments
            }
            discussionComments.add(c)
        }
        loggingService.log("Comment cache is loaded with ${tempComments.size}")
    }

    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        val list: List<Comment> = commentsByDiscussion[discussionId] ?: return emptyList()
        val normalizedEnd: Int = if (end > list.size) { list.size} else { end }

        return list.subList(start, normalizedEnd)
    }

    fun save(comment: Comment) {
        var list: MutableList<Comment>? = commentsByDiscussion[comment.discussionId]
        if (list == null) {
            list = mutableListOf()
            commentsByDiscussion[comment.discussionId] = list
        }
        list.add(comment)
    }
}