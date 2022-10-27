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
    private var commentsByDiscussion: MutableMap<Long, MutableList<Comment>> = ConcurrentHashMap<Long, MutableList<Comment>>()
    private var comments: MutableMap<Long, Comment> = ConcurrentHashMap<Long, Comment>()
    private var commentsByJobId: MutableMap<String, Comment> = ConcurrentHashMap<String, Comment>()

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
            comments[c.id] = c
            if (c.conversionJobId != null) {
                commentsByJobId.put(c.conversionJobId!!, c)
            }
        }
        loggingService.log("Comment cache is loaded with ${tempComments.size}")
    }

    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        synchronized(commentsByDiscussion) {
            val list: List<Comment> = commentsByDiscussion[discussionId] ?: return emptyList()
            val normalizedEnd: Int = if (end > list.size) { list.size} else { end }

            val temp: MutableList<Comment> = mutableListOf()
            for(c in list.subList(start, normalizedEnd)) {
                temp.add(c)
            }
            return temp
        }
    }

    fun save(comment: Comment) {
        val updatedComment = commentRepository.save(comment)

        synchronized(this.commentsByDiscussion) {
            var list: MutableList<Comment>? = commentsByDiscussion[updatedComment.discussionId]
            if (list == null) {
                list = mutableListOf()
                commentsByDiscussion[updatedComment.discussionId] = list
            }

            /**TODO: Needs a unit test**/
            val position = list.indexOf(updatedComment)
            if (position >= 0) {
                list.add(position, updatedComment)
            } else {
                list.add(updatedComment)
            }

            comments[comment.id] = updatedComment
            if(comment.conversionJobId != null) {
                commentsByJobId[comment.conversionJobId!!] = comment
            }
        }
    }


    fun get(id: Long) : Comment? {
        return comments[id]
    }

    fun getByJobId(jobId: String) : Comment?  {
        return commentsByJobId[jobId]
    }
}