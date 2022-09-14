package com.bloip.services

import com.bloip.caches.DiscussionCache
import com.bloip.domain.*
import com.bloip.repositories.DiscussionRepository
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(
        @Autowired val discussionCache: DiscussionCache,
        @Autowired val commentService: CommentService,
        @Autowired val discussionRepository: DiscussionRepository,
        @Autowired val userService: UserService,
        @Autowired val inboxService: InboxService,
        @Autowired val discussionSubscriptionService: DiscussionSubscriptionService
    ) {
    fun getNextPage(offsetKey: Long?) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getNextPage(offsetKey)
    }
    fun getPreviousPage(offsetKey: Long) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getPreviousPage(offsetKey)
    }

    fun get(discussionId: Long): Discussion? {
        return discussionCache.get(discussionId)
    }

    @Transactional
    fun create(userId: Long, title: String, topic: Topic, ipAddress: String): Discussion {
        val user: User = userService.findById(userId)!!
        var discussion = discussionRepository.save(
            Discussion(
                userId    = user.id,
                title     = title,
                topic   = topic,
                ipAddress = ipAddress
            )
        )
        val comment = Comment(
            userId       = user.id,
            discussionId = discussion.id,
            audioUrl     = "https://www.w3schools.com/html/horse.mp3",
            trackNumber  = 0,
            ipAddress    = ipAddress
        )
        commentService.save(comment)
        save(discussion)

        discussionCache.push(discussion)
        subscribe(discussionId = discussion.id, userId = userId)

        return discussion
    }

    //TODO: What if a user is replying to a discussion that was just deleted/banned?
    @Transactional
    fun reply(userId: Long, discussionId: Long, ipAddress: String) : Comment {
        val discussion: Discussion = discussionCache.get(discussionId)!!
        discussion.numberOfReplies++

        val user: User = userService.findById(userId)!!
        val comment = Comment(
            userId       = user.id,
            discussionId = discussion.id,
            audioUrl     = "https://www.w3schools.com/html/horse.mp3",
            trackNumber  = discussion.numberOfReplies,
            ipAddress    = ipAddress
        )

        discussionCache.bump(discussionId = discussionId)
        val updatedComment = commentService.save(comment)
        save(discussion)
        subscribe(discussionId = discussion.id, userId = userId)

        inboxService.updateSubscriberInboxes(senderId = userId, discussion = discussion, trackNumber = comment.trackNumber)

        return updatedComment
    }

    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        return commentService.getComments(discussionId, start, end)
    }

    fun unsubscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.unsubscribe(discussionId, userId)
        inboxService.toggleInboxSubscriptionIfInboxItemExists(userId = userId, discussionId = discussionId, false)
    }

    /** You can unsubscribed from an inbox item to stop notifications but keep it in your inbox to refer to later.
     *  Users can also resubscribe to the same inbox item, which is just a conversation. In this sense an inbox
     *  can act as a feed of sorts.
     */
    fun subscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.subscribe(discussionId, userId)
        inboxService.toggleInboxSubscriptionIfInboxItemExists(userId = userId, discussionId = discussionId, true)
    }

    /** The date needs to be modified after changing a reply so that discussions are bumped on disk not just in the cache **/
    fun save(discussion: Discussion) {
        discussion.updateTimestamp = Date()
        discussionRepository.save(discussion)
    }
}