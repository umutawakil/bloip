package com.bloip.services

import com.bloip.caches.DiscussionCache
import com.bloip.domain.*
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Title
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.webpush.WebPushService
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
        @Autowired private val discussionCache: DiscussionCache,
        @Autowired private val commentService: CommentService,
        @Autowired private val discussionRepository: DiscussionRepository,
        @Autowired private val userService: UserService,
        @Autowired private val inboxService: InboxService,
        @Autowired private val discussionSubscriptionService: DiscussionSubscriptionService,
        @Autowired private val webPushService: WebPushService
    ) {
    fun getNextPage(topicFriendlyId: String?, offsetKey: Long?) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getNextPage(topicFriendlyId = topicFriendlyId, offsetKey)
    }
    fun getPreviousPage(topicFriendlyId: String?, offsetKey: Long) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getPreviousPage(topicFriendlyId = topicFriendlyId, offsetKey)
    }

    fun get(discussionId: Long): Discussion? {
        return discussionCache.get(discussionId)
    }

    @Transactional
    fun create(userId: Long, title: Title, topic: Topic, ipAddress: String, duration: Int, fileName: String): Discussion {
        val user: User = userService.findById(userId)!!
        val discussion = discussionRepository.save(
            Discussion(
                userId    = user.id,
                title     = title,
                topic     = topic,
                ipAddress = ipAddress,
                fileName  = fileName
            )
        )
        val comment = Comment(
            userId       = user.id,
            discussionId = discussion.id,
            fileName     = fileName,
            trackNumber  = 0,
            ipAddress    = ipAddress,
            duration     = duration
        )
        commentService.save(comment)

        subscribe(discussionId = discussion.id, userId = userId)
        discussionCache.push(discussion)

        return discussion
    }

    //TODO: What if a user is replying to a discussion that was just deleted/banned?
    @Transactional
    fun reply(userId: Long, discussionId: Long, ipAddress: String, duration: Int, fileName: String) : Comment {
        val discussion: Discussion = discussionCache.get(discussionId)!!
        discussion.numberOfReplies++

        val user: User = userService.findById(userId)!!
        val comment = Comment(
            userId       = user.id,
            discussionId = discussion.id,
            fileName     = fileName,
            trackNumber  = discussion.numberOfReplies,
            ipAddress    = ipAddress,
            duration     = duration
        )

        val updatedComment = commentService.save(comment)
        updateDiscussionTimestampWithSave(discussion)
        subscribe(discussionId = discussion.id, userId = userId)

        /** Utilized for site inbox notifications and webpush notifications and the time of this writing **/
        val subscriberUserIds = discussionSubscriptionService.getSubscribers(discussionId = discussion.id)

        inboxService.updateSubscriberInboxes(
            senderId    = userId,
            discussion  = discussion,
            trackNumber = comment.trackNumber,
            userIds     = subscriberUserIds
        )

        /** We dont send web push notifications synchronously to avoid spamming people. Instead we toggle a flag associated with
         * each web push subscription and that signifies that a notification is in need for a given user.
         * The rules of when to send that notification are
         * governed by an async task that will probably move to a job outside of the code eventually.**/
        webPushService.scheduleWebPush(userIds = subscriberUserIds)

        discussionCache.bump(discussionId = discussionId)
        return updatedComment
    }

    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        return commentService.getComments(discussionId, start, end)
    }

    fun unsubscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.unsubscribe(discussionId, userId)
        inboxService.toggleInboxSubscriptionIfInboxItemExists(userId = userId, discussionId = discussionId, false)
    }

    /** You can unsubscribe from an inbox item to stop notifications but keep it in your inbox to refer to later.
     *  Users can also resubscribe to the same inbox item, which is just a conversation. In this sense an inbox
     *  can act as a feed of sorts.
     */
    fun subscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.subscribe(discussionId, userId)
        inboxService.toggleInboxSubscriptionIfInboxItemExists(userId = userId, discussionId = discussionId, true)
    }

    /** The date needs to be modified after changing a reply so that discussions are bumped on disk not just in the cache **/
    fun updateDiscussionTimestampWithSave(discussion: Discussion) {
        discussion.updateTimestamp = Date()
        discussionRepository.save(discussion)
    }

    fun getSize() : Int {
        return discussionCache.getSize()
    }
}