package com.bloip.services

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.mediaconvert.model.CreateJobRequest
import com.amazonaws.services.mediaconvert.model.CreateJobResult
import com.bloip.caches.DiscussionCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.*
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.DiscussionSubscription
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
import org.springframework.ui.Model
import java.util.*

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@Service
class DiscussionService(
        @Autowired private val adminService: AdminService,
        @Autowired private val discussionCache: DiscussionCache,
        @Autowired private val discussionRepository: DiscussionRepository,
        @Autowired var mediaConversionService: AudioConversionRequestService,
        @Autowired private val applicationProperties: ApplicationProperties
    ) {
    fun getNextPage(country: Country, offsetKey: Long?) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getNextPage(country = country, offsetKey)
    }
    fun getPreviousPage(country: Country, offsetKey: Long) : BumpStack.Page<Long, Discussion> {
        return discussionCache.getPreviousPage(country = country, offsetKey)
    }

    fun get(discussionId: Long?): Discussion? {
        if(discussionId == null) return null

        return discussionCache.get(discussionId)
    }

    fun create(
        user: User,
        title: Title,
        duration: Int,
        fileName: String,
        youtubeLink: YoutubeLink? = null,
        country: Country,
        eventSequenceId: String
    ): Discussion {
        val start = System.nanoTime()
        if (user.isDiscussionCreationLimitReached()) {
            throw RuntimeException("Max limit of daily discussions has been reached")
        }

        var updatedUser: User = user
        if (user.shouldResetCreationWindow()) {
            updatedUser = user.resetDiscussionCreationWindow()
        }

        val discussion = update(
            update(
                Discussion(
                    userId          = updatedUser.id,
                    title           = title,
                    fileName        = fileName,
                    youtubeLink     = youtubeLink,
                    needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName),
                    country         = country
                )
            ).addComment(
                trackNumber     = 0,
                userId          = updatedUser.id,
                fileName        = fileName,
                duration        = duration,
                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
            )
        )

        /** Double save is needed since Comment does not use references to Discussion and instead uses Ids **/
        /*var updatedDiscussion = update(
            discussion.addComment(
                userId          = updatedUser.id,
                fileName        = fileName,
                duration        = duration,
                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
            )
        )*/
        var updatedDiscussion = discussion

        if (updatedDiscussion.lastCommentNeedsConversion()) {
            updatedDiscussion.convertLastComment(mediaConversionService = mediaConversionService)
        }

        updatedDiscussion = subscribe(discussion = updatedDiscussion, user = updatedUser)
        updatedUser       = User.findById(userId = updatedUser.id)!!
        updatedUser       = updatedUser.updateDiscussionLimitStats()

        adminService.recordEvent(
            eventMessage = "New Discussion created: "+
                    updatedDiscussion.title+"\r\n"+
                    updatedDiscussion.getEnglishUrl()
        )
        UserEvent(
            name               = "create",
            methodName         = "create",
            context            = "discussion_service",
            durationInNanoSecs = (System.nanoTime() - start) * 0.0,
            user               = updatedUser,
            sequenceId         = eventSequenceId,
            sequenceComplete   = false
        ).saveNow()

        return updatedDiscussion
    }

    //TODO: What if a user is replying to a discussion that was just deleted/banned?

    fun reply(user: User, discussion: Discussion, duration: Int, fileName: String, eventSequenceId: String) : Discussion {
        val start = System.nanoTime()
        discussion.numberOfReplies++

        /** Update the link on the homepage to reflect the latest comment **/
        discussion.fileName        = fileName
        discussion.needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)

        /** Users can not post again till someone else replies **/
        if(discussion.isLastUserToComment(user)) {
            throw RuntimeException("User has not received a response but is replying")
        }
        discussion.lastUserId = user.id

        /** The last bad record is being overwritten **/
        if(discussion.censured) {
            discussion.censured = false
        }

        var updatedDiscussion: Discussion = update(
            discussion.addComment(
                userId          = user.id,
                fileName        = fileName,
                duration        = duration,
                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName),
                trackNumber     = discussion.getLatestTrackNumber()
            )
        )
        //var updatedDiscussion: Discussion = update(discussion = discussion)

        updatedDiscussion = subscribe(discussion = updatedDiscussion, user = user)
        val updatedUser   = User.findById(userId = user.id)!!

        //discussionSubscriptionService.subscribe(discussion, user)
        /*if (inboxService.getInboxItem(discussionId = discussionId, userId = userId) != null) {
            inboxService.toggleInboxSubscription(userId = userId, discussionId = discussionId, value = true)
        }*/
        //val subscriberUserIds = discussionSubscriptionService.getSubscribers(discussionId = discussion.id)

        User.updateSubscriberInboxes(
            sender      = updatedUser,
            discussion  = updatedDiscussion,
            trackNumber = updatedDiscussion.getLatestTrackNumber(),
            users       = updatedDiscussion.subscribers.mapNotNull { User.findById(it.userId) }.toSet()
        )

        bump(discussion = updatedDiscussion)

        if(updatedDiscussion.lastCommentNeedsConversion()) {
            updatedDiscussion.convertLastComment(mediaConversionService)
        }

        adminService.recordEvent(
            eventMessage = "New reply created: "+
                    updatedDiscussion.title + "\r\n"+
                    updatedDiscussion.getEnglishUrl()+
                    "?trackNumber=" + updatedDiscussion.numberOfReplies
        )

        UserEvent(
            name               = "reply",
            methodName         = "reply",
            context            = "discussion_service",
            durationInNanoSecs = (System.nanoTime() - start) * 0.0,
            user               = updatedUser,
            sequenceId         = eventSequenceId,
            sequenceComplete   = false,
        ).saveNow()

        return updatedDiscussion
    }

    fun getView(discussionId: Long, start: Int, end: Int) : Any? {
        val discussion: Discussion = discussionCache.get(discussionId = discussionId) ?: return null
        return discussion.getView(start = start, end = end)
    }

    fun unsubscribe(user: User, discussion: Discussion) {
        //discussionSubscriptionService.unsubscribe(discussionId, userId)
        var toBeDeleted: DiscussionSubscription? = null
        for(s in discussion.subscribers) {
            if(s.discussionId == discussion.id && s.userId == user.id) {
                toBeDeleted = s
                break
            }
        }
        if (toBeDeleted != null) {
            discussion.subscribers.remove(toBeDeleted)
        }
        val updatedDiscussion = update(discussion = discussion)

        user.toggleInboxSubscription(
            discussion = updatedDiscussion,
            value = false
        )
    }

    /** You can unsubscribe from an inbox item to stop notifications but keep it in your inbox to refer to later.
     *  Users can also resubscribe to the same inbox item, which is just a conversation. In this sense an inbox
     *  can act as a feed of sorts.
     */
    fun subscribe(discussion: Discussion, user: User) : Discussion {
        //discussionSubscriptionService.subscribe(discussionId, userId)
        if (discussion.subscribers.find { x -> x.userId == user.id} == null) {
            discussion.subscribers.add(
                DiscussionSubscription(
                    discussionId = discussion.id,
                    userId       = user.id
                )
            )
        } else {
            return discussion
        }
        val updatedDiscussion = update(discussion = discussion)
        user.toggleInboxSubscription(discussion = updatedDiscussion, value = true)

        return updatedDiscussion
    }

    fun update(discussion: Discussion) : Discussion {
        discussion.updateTimestamp = Date()
        val updatedDiscussion = discussionRepository.save(discussion)
        if (!discussionCache.contains(discussion)) {
            discussionCache.push(
                updatedDiscussion
            )
        } else {
            discussionCache.update(
                updatedDiscussion
            )
        }
        return updatedDiscussion
    }

    fun bump(discussion: Discussion) {
        discussionCache.bump(discussionId = discussion.id)
    }

    /** Automated testing in dev only **/
    fun deleteAll() {
        if (applicationProperties.environment != "dev") {
            throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
        }

        discussionCache.deleteAll()
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }
    }

    fun censureTitle(discussion: Discussion) {
        update(discussion.censureTitle())
    }

    fun censureComment(discussion: Discussion, trackNumber: Int) {
        discussion.censureComment(trackNumber = trackNumber)
        update(discussion)
    }

    fun censureUser(discussion: Discussion, trackNumber: Int) {
        discussion.censureUser(trackNumber = trackNumber)
        update(discussion)
        censureComment(discussion = discussion, trackNumber = trackNumber)
    }

    fun displayComment(discussion: Discussion, trackNumber: Int, model: Model) {
        discussion.displaySingleComment(model = model, trackNumber = trackNumber)
    }

    fun updateWithJobInfo(jobId: String, discussion: Discussion, trackNumber: Int) {
        discussionCache.updateWithJobInfo(
            trackNumber = trackNumber,
            discussion  = update(discussion = discussion),
            jobId       = jobId
        )
    }

    fun getByJobInfo(jobId: String) : Pair<Int, Discussion>? {
        return discussionCache.getByJobInfo(jobId = jobId)
    }

    private class MediaConvertHandler(
        private val trackNumber: Int,
        private val discussion: Discussion,
        private val discussionService: DiscussionService,
        private val loggingService: LoggingService
    ) : AsyncHandler<CreateJobRequest, CreateJobResult> {

        override fun onError(exception: Exception) {
            loggingService.error("Error sending request to AWS Media convert", exception)
        }

        override fun onSuccess(request: CreateJobRequest, result: CreateJobResult) {
            discussion.conversionJobId = result.job.id
            discussion.audioConversionInProgress = true
            discussionService.updateWithJobInfo(trackNumber = trackNumber,
                discussion = discussion.conversionJobStarted(
                        trackNumber = trackNumber, jobId = result.job.id
                ),
                jobId = result.job.id
            )
        }
    }

    fun buildMediaConvertHandler(
        trackNumber: Int,
        discussion: Discussion,
        discussionService: DiscussionService,
        loggingService: LoggingService
    ) : AsyncHandler<CreateJobRequest, CreateJobResult> {

        return MediaConvertHandler(
            trackNumber       = trackNumber,
            discussion        = discussion,
            discussionService = discussionService,
            loggingService    = loggingService
        )
    }

    fun conversionComplete(discussion: Discussion, trackNumber: Int) {
        update(
            discussion = discussion.conversionComplete(
                trackNumber = trackNumber
            )
        )
    }
}