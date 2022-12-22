package com.bloip.services

import com.bloip.caches.DiscussionCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.*
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.discussion.value.YoutubeLink
import com.bloip.domain.localization.Country
import com.bloip.domain.user.User
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.admin.AdminService
import com.bloip.services.audioconversion.AudioConversionRequestService
import com.bloip.structures.BumpStack
import com.bloip.utilities.DiscussionUtility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(
    @Autowired private val adminService: AdminService,
    @Autowired private val discussionCache: DiscussionCache,
    @Autowired private val commentService: CommentService,
    @Autowired private val discussionRepository: DiscussionRepository,
    @Autowired private val userService: UserService,
    @Autowired private val inboxService: InboxService,
    @Autowired private val discussionSubscriptionService: DiscussionSubscriptionService,
    @Autowired var mediaConversionService: AudioConversionRequestService,
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService
    ) {
    fun getNextPage(country: Country, offsetKey: Long?) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getNextPage(country = country, offsetKey)
    }
    fun getPreviousPage(country: Country, offsetKey: Long) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getPreviousPage(country = country, offsetKey)
    }

    fun get(discussionId: Long): Discussion? {
        return discussionCache.get(discussionId)
    }

    @Transactional
    fun create(
        userId: Long,
        title: Title,
        ipAddress: String,
        duration: Int,
        fileName: String,
        youtubeLink: YoutubeLink? = null,
        country: Country,
        eventSequenceId: String
    ): Discussion {
        val start = System.nanoTime()
        var user: User = userService.findById(userId)!!
        if (userService.isDiscussionCreationLimitReached(user)) {
            throw RuntimeException("Max limit of daily discussions has been reached")
        }
        if (userService.shouldResetCreationWindow(user)) {
            user = userService.resetDiscussionCreationWindow(user = user)
        }

        val discussion = update(
            Discussion(
                userId          = user.id,
                title           = title,
                ipAddress       = ipAddress,
                fileName        = fileName,
                youtubeLink     = youtubeLink,
                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName),
                country         = country
            )
        )
        val comment = commentService.save(
            Comment(
                userId          = user.id,
                discussionId    = discussion.id,
                fileName        = fileName,
                trackNumber     = 0,
                ipAddress       = ipAddress,
                duration        = duration,
                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
            )
        )

        /** TODO: This needs a better distinction with the public method that toggles the inbox.
         * Subscribe for the first time creating the subscription.
         * After this point the subscription process and unsubscription process will be a toggle **/
        discussionSubscriptionService.subscribe(discussion.id, userId)

        discussionCache.push(discussion)

        if (comment.needsConversion) {
            mediaConversionService.startConvertingAudioFile(
                comment = comment
            )
        }

        userService.updateDiscussionLimitStats(user)

        adminService.recordEvent(
            eventMessage = "New Discussion created: "+
                    discussion.title+"\r\n"+
                    discussion.getEnglishUrl()
        )

        UserEvent(
            name               = "create",
            methodName         = "create",
            context            = "discussion_service",
            durationInNanoSecs = (System.nanoTime() - start) * 0.0,
            user               = user,
            sequenceId         = eventSequenceId,
            sequenceComplete   = false,
        ).saveNow()

        return discussion
    }

    //TODO: What if a user is replying to a discussion that was just deleted/banned?
    @Transactional
    fun reply(userId: Long, discussionId: Long, ipAddress: String, duration: Int, fileName: String, eventSequenceId: String) : Comment {
        val start = System.nanoTime()
        val discussion: Discussion = discussionCache.get(discussionId)!!
        discussion.numberOfReplies++

        /** Update the link on the homepage to reflect the latest comment **/
        discussion.fileName        = fileName
        discussion.needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)

        /** Users can not post again till someone else replies **/
        if(userId == discussion.lastUserId) {
            throw RuntimeException("User has not received a response but is replying")
        }
        discussion.lastUserId = userId

        /** The last bad record is being overwritten **/
        if(discussion.censured) {
            discussion.censured = false
        }

        val user: User = userService.findById(userId)!!
        val comment = Comment(
            userId          = user.id,
            discussionId    = discussion.id,
            fileName        = fileName,
            trackNumber     = discussion.numberOfReplies,
            ipAddress       = ipAddress,
            duration        = duration,
            needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
        )

        val updatedComment = commentService.save(comment)
        updateDiscussionTimestampWithSave(discussion)

        /** TODO: This needs a better distinction with the public method that toggles the inbox.
         * Subscribe for the first time creating the subscription.
         * After this point the subscription process and unsubscription process will be a toggle **/
        discussionSubscriptionService.subscribe(discussionId, userId)

        /** Replying to a discussion you unsubscribed to automatically resubscribes you **/
        if (inboxService.getInboxItem(discussionId = discussionId, userId = userId) != null) {
            inboxService.toggleInboxSubscription(userId = userId, discussionId = discussionId, value = true)
        }

        /** Utilized for site inbox notifications and webpush notifications and the time of this writing **/
        val subscriberUserIds = discussionSubscriptionService.getSubscribers(discussionId = discussion.id)

        inboxService.updateSubscriberInboxes(
            senderId    = userId,
            discussion  = discussion,
            trackNumber = updatedComment.trackNumber,
            userIds     = subscriberUserIds
        )

        bump(discussionId = discussionId)

        if(updatedComment.needsConversion) {
            mediaConversionService.startConvertingAudioFile(
                comment           = updatedComment,
            )
        }

        adminService.recordEvent(
            eventMessage = "New reply created: "+
                    discussion.title + "\r\n"+
                    discussion.getEnglishUrl()+
                    "?trackNumber=" + updatedComment.trackNumber
        )

        UserEvent(
            name               = "reply",
            methodName         = "reply",
            context            = "discussion_service",
            durationInNanoSecs = (System.nanoTime() - start) * 0.0,
            user               = user,
            sequenceId         = eventSequenceId,
            sequenceComplete   = false,
        ).saveNow()

        return updatedComment
    }

    fun getComments(discussionId: Long, start: Int, end: Int) : List<Comment> {
        return commentService.getComments(discussionId, start, end)
    }

    @Transactional
    fun unsubscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.unsubscribe(discussionId, userId)
        inboxService.toggleInboxSubscription(userId = userId, discussionId = discussionId, false)
    }

    /** You can unsubscribe from an inbox item to stop notifications but keep it in your inbox to refer to later.
     *  Users can also resubscribe to the same inbox item, which is just a conversation. In this sense an inbox
     *  can act as a feed of sorts.
     */
    @Transactional
    fun subscribe(discussionId: Long, userId: Long) {
        discussionSubscriptionService.subscribe(discussionId, userId)
        inboxService.toggleInboxSubscription(userId = userId, discussionId = discussionId, true)
    }

    /** The date needs to be modified after changing a reply so that discussions are bumped on disk not just in the cache **/
    fun updateDiscussionTimestampWithSave(discussion: Discussion) {
        discussion.updateTimestamp = Date()
        update(discussion)
    }

    fun update(discussion: Discussion) : Discussion {
        val updatedDiscussion =  discussionRepository.save(discussion)
        discussionCache.update(updatedDiscussion)
        return  updatedDiscussion
    }

    fun bump(discussionId: Long) {
        discussionCache.bump(discussionId = discussionId)
    }

    /** Automated testing in deve only **/
    fun deleteAll() {
        if (applicationProperties.environment != "dev") {
            throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
        }

        discussionCache.deleteAll()
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }
    }
}