package com.bloip.services

import com.bloip.caches.DiscussionCache
import com.bloip.domain.*
import com.bloip.repositories.DiscussionRepository
import com.bloip.structures.BumpStack
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(
        @Autowired val discussionCache: DiscussionCache,
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
            audioUrl = "",
            trackNumber = 0,
            ipAddress = ipAddress
        )
        discussion.comments.add(comment)
        discussionRepository.save(discussion)

        discussionCache.push(discussion)
        discussionSubscriptionService.subscribe(discussionId = discussion.id, userId = userId)

        return discussion
    }

    //TODO: What if a user is replying to a discussion that was just deleted/banned?
    @Transactional
    fun reply(userId: Long, discussionId: Long, ipAddress: String): Discussion {
        val discussion: Discussion = discussionCache.get(discussionId)!!
        discussion.numberOfReplies++

        val user: User = userService.findById(userId)!!
        val comment = Comment(
            user = user,
            discussion = discussion,
            audioUrl = "",
            trackNumber = discussion.numberOfReplies + 1,
            ipAddress = ipAddress
        )

        discussion.comments.add(comment)
        val updatedDiscussion = discussionRepository.save(discussion)

        discussionSubscriptionService.subscribe(discussionId = discussion.id, userId = userId)

        inboxService.updateSubscriberInboxes(senderId = userId, discussion = discussion, trackNumber = comment.trackNumber)

        return updatedDiscussion
    }

}