package com.bloip.domain.discussion

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.mediaconvert.model.*
import com.bloip.configuration.ApplicationProperties
import com.bloip.configuration.EnvironmentConfigs
import com.bloip.domain.localization.Country
import com.bloip.domain.StandardDomainObject
import com.bloip.domain.UserEvent
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.discussion.value.YoutubeLink
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User.UserId
import com.bloip.domain.user.User
import com.bloip.exceptions.DiscussionDoesNotExistForReply
import com.bloip.repositories.GenericRepository

import com.bloip.services.LoggingService
import com.bloip.services.admin.AdminService
import com.bloip.services.audioconversion.AudioConversionRequestService
import com.bloip.structures.BumpStack
import com.bloip.utilities.DiscussionUtility
import com.bloip.utilities.EntityManagementUtils
import com.bloip.utilities.WebUtil
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import org.hibernate.Session
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.ui.ExtendedModelMap
import org.springframework.ui.Model
import org.springframework.ui.set
import java.io.Serializable
import java.util.Date
import java.util.concurrent.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.PostConstruct
import javax.persistence.*
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

@Table(name = "discussion")
@Entity
class Discussion {
    @Component
    private class SpringAdapter(
        @Autowired private val adminService:           AdminService,
        @Autowired private var mediaConversionService: AudioConversionRequestService,
        @Autowired private val applicationProperties:  ApplicationProperties,
        @Autowired private val loggingService:         LoggingService,
        @Autowired private val entityManagerFactory:   EntityManagerFactory,
        @Autowired private val genericRepository:      GenericRepository,
    ) {

        @PostConstruct
        fun init() {
            Discussion.adminService           = adminService
            Discussion.mediaConversionService = mediaConversionService
            Discussion.applicationProperties  = applicationProperties
            Discussion.loggingService         = loggingService
            Discussion.entityManagerFactory   = entityManagerFactory
            Discussion.genericRepository      = genericRepository

            /** Initialize discussions **/
            val discussionResults: List<Discussion> = EntityManagementUtils.
            getSession(Companion.entityManagerFactory).
            createQuery("SELECT d FROM Discussion d ORDER BY d.creationTimestamp ASC").resultList as List<Discussion>
            for(d: Discussion in discussionResults) {
                discussions[d.id] = d
            }
            loggingService.log("Discussion cache initialized: ${discussions.size} discussions loaded.")

            /** Initialize comments**/
            Comment.init()

            /** Initialize subscriptions **/
            Subscription.init()
        }
    }

    private class DiscussionDTO {
        /** Private **/
        val censured: Boolean
        val needsConversion: Boolean

        /** Public **/
        val id: DiscussionId
        val title: Title
        val fileName: String
        val numberOfReplies: Int
        val updateTimestamp: Date

        constructor(id: DiscussionId, needsConversion: Boolean, censured: Boolean, title: Title, fileName: String, numberOfReplies: Int, updateTimestamp: Date) {
            this.id              = id
            this.needsConversion = needsConversion
            this.censured        = censured
            this.title           = title
            this.fileName        = fileName
            this.numberOfReplies = numberOfReplies
            this.updateTimestamp = updateTimestamp
        }

        //TODO: Needs a unit test
        val audioUrl: String
            get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
                needsConversion = this.needsConversion,
                fileName        = this.fileName
            ) } else {
                EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
            }

        fun getUrl(language: Language): String {
            //return "/d/" + this.id + "/l/" + language.code
            return getUrl(discussionId = this.id, language = language)
        }

        fun getEnglishUrl() : String {
            //return "/d/" + this.id + "/l/en"
            return getEnglishUrl(discussionId = this.id)
        }

        fun testVerifyTitleContains(title: String) : Boolean {
            return this.title.value.contains(title)
        }

    }
    companion object {
        private lateinit var adminService: AdminService
        private lateinit var mediaConversionService: AudioConversionRequestService
        private lateinit var applicationProperties: ApplicationProperties
        private lateinit var loggingService: LoggingService
        private lateinit var entityManagerFactory: EntityManagerFactory
        private lateinit var genericRepository: GenericRepository

        private val discussions: MutableMap<DiscussionId, Discussion> = ConcurrentHashMap<DiscussionId, Discussion>()
        private val discussionLocks: MutableMap<DiscussionId, Lock>   = ConcurrentHashMap()
        fun create(
            userId: UserId,
            title: Title,
            duration: Int,
            fileName: String,
            youtubeLink: YoutubeLink? = null,
            country: Country,
            eventSequenceId: String
        ): Discussion {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val start = System.nanoTime()
                if (
                    User.isDiscussionCreationLimitReached(
                        userId = userId
                    )
                )
                {
                    throw RuntimeException("Max limit of daily discussions has been reached")
                }

                val discussion: Discussion = saveWithSession(
                    session    = session,
                    discussion = Discussion
                                 (
                                    userId      = userId,
                                    title       = title,
                                    youtubeLink = youtubeLink,
                                    country     = country
                                 )
                )

                val comment: Comment = Comment.saveWithSession(
                    session = session,
                    comment = Comment
                                    (
                                        discussionId    = discussion.id,
                                        country         = discussion.country,
                                        trackNumber     = 1,
                                        userId          = userId,
                                        fileName        = fileName,
                                        duration        = duration,
                                        needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
                                    )
                )

                Subscription.replaceSubscription(
                    session           = session,
                    discussionId      = discussion.id,
                    userId            = userId,
                    latestTrackNumber = discussion.getLatestTrackNumber()
                )

                Comment.convertCommentIfNeededAsync(
                    comment                = comment,
                    eventSequenceId        = eventSequenceId,
                    mediaConversionService = mediaConversionService
                )

                User.updateDiscussionLimitStats(session = session,userId = userId)

                adminService.recordEvent(
                    eventMessage = "New Discussion created: " +
                            discussion.title + "\r\n" +
                            discussion.getEnglishUrl()
                )
                UserEvent(
                    name               = "create",
                    methodName         = "create",
                    context            = "discussion_service",
                    durationInNanoSecs = (System.nanoTime() - start) * 0.0,
                    userId             = userId,
                    sequenceId         = eventSequenceId,
                    sequenceComplete   = false
                ).asyncSave()

                tx.commit()
                return discussion

            } finally {
                session.close()
            }
        }

        private fun saveWithSession(session: Session, discussion: Discussion) : Discussion {
            val updatedDiscussion: Discussion = session.merge(discussion) as Discussion
            session.flush()

            discussions[updatedDiscussion.id] = updatedDiscussion
            return updatedDiscussion
        }

        //TODO: What if a user is replying to a discussion that was just deleted/banned?
        fun reply(
            userId: UserId,
            discussionId: DiscussionId,
            duration: Int,
            fileName: String,
            eventSequenceId: String
        ) : Discussion {
            val lock: Lock = discussionLocks.computeIfAbsent(discussionId) { ReentrantLock(true)}
            lock.lock()
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val discussion: Discussion = get(discussionId) ?: throw DiscussionDoesNotExistForReply(discussionId)
                val start = System.nanoTime()
                val newTrackNumber = discussion.getLatestTrackNumber() + 1

                /** Users can not post again till someone else replies **/
                if (discussion.isLastUserToComment(userId)) {
                    throw RuntimeException("User has not received a response but is replying")
                }

                val comment: Comment = Comment.saveWithSession(
                    session = session,
                    comment = Comment(
                                discussionId    = discussionId,
                                country         = discussion.country,
                                trackNumber     = newTrackNumber,
                                userId          = userId,
                                fileName        = fileName,
                                duration        = duration,
                                needsConversion = DiscussionUtility.fileNeedsToBeConverted(fileName)
                            )
                )

                Subscription.replaceSubscription(
                    session           = session,
                    discussionId      = discussionId,
                    userId            = userId,
                    latestTrackNumber = newTrackNumber
                )

                bump(discussionId = discussionId, country = discussion.country)

                Comment.convertCommentIfNeededAsync(
                    comment                = comment,
                    eventSequenceId        = eventSequenceId,
                    mediaConversionService = mediaConversionService
                )

                Subscription.notifyApplicableUsers(
                    discussionId = discussionId
                )

                adminService.recordEvent(
                    eventMessage = "New reply created: " +
                            discussion.title + "\r\n" +
                            discussion.getEnglishUrl() +
                            "?trackNumber=" + newTrackNumber
                )

                UserEvent(
                    name               = "reply",
                    methodName         = "reply",
                    context            = "discussion_service",
                    durationInNanoSecs = (System.nanoTime() - start) * 0.0,
                    userId             = userId,
                    sequenceId         = eventSequenceId,
                    sequenceComplete   = false,
                ).asyncSave()

                tx.commit()

                return discussion
            } finally {
                session.close()
                lock.unlock()
            }
        }

        fun get(discussionId: DiscussionId?): Discussion? {
            if(discussionId == null) return null
            return discussions[discussionId]
        }

        fun getForDisplay(discussionId: DiscussionId?): Any? {
            if (discussionId == null) return null
            val d: Discussion = get(discussionId) ?: return null

            return Comment.getForDisplay(
                discussionId    = discussionId,
                title           = d.title,
                numberOfReplies = d.getNumberOfReplies()
            )
        }

        fun getNextPage(country: Country, offsetKey: DiscussionId?) : BumpStack.Page<DiscussionId, Any> {
            return Comment.getNextPage(country = country, offSetKey = offsetKey)
        }
        fun getPreviousPage(country: Country, offsetKey: DiscussionId) : BumpStack.Page<DiscussionId, Any> {
            return Comment.getPreviousPage(country = country, offsetKey)
        }

        fun getCommentsView(discussionId: DiscussionId, start: Int, end: Int) : Any? {
            val discussion: Discussion = get(discussionId = discussionId) ?: return null
            return discussion.getCommentsView(start = start, end = end)
        }

        fun subscribe(discussionId: DiscussionId, userId: UserId) {
            Subscription.subscribe(discussionId, userId)
        }

        fun leaveConversation(discussionId: DiscussionId, userId: UserId) {
            Subscription.leaveConversation(discussionId, userId)
        }

        fun resetUnreadConversationIndicator(userId: UserId,discussionId: DiscussionId) {
            Subscription.resetUnreadConversationIndicator(userId, discussionId)
        }

        fun showInboxTotal(userId: UserId, httpServletResponse: HttpServletResponse) {
            Subscription.showInboxTotal(userId = userId, httpServletResponse = httpServletResponse)
        }

        fun showInboxPage(userId: UserId, model: Model, offset: Int?, itemsPerPage: Int) {
            Subscription.showInboxPage(userId = userId, model = model, offset = offset, itemsPerPage = itemsPerPage)
        }

        fun setInboxTotalInSession(userId: UserId, httpSession: HttpSession) {
            Subscription.setInboxTotalInSession(userId = userId, httpSession = httpSession)
        }

        /** Automated testing in dev only **/
        fun deleteAll() {
            if (applicationProperties.environment != "dev") {
                throw RuntimeException("Function can only be used in dev, specifically for integration testing!!!")
            }
            for (d in findAll()) {
                delete(discussionId = d.id)
            }
        }

        fun findAll() : Collection<Discussion> {
            return discussions.values
        }

        fun delete(discussionId: DiscussionId) {
            val session: Session = getSession()
            val tx = session.beginTransaction()
            try {
                val discussion: Discussion = get(discussionId)!!
                session.delete(session.merge(discussion))
                discussions.remove(discussionId)
                tx.commit()

                Comment.delete(
                    country      = discussion.country,
                    discussionId = discussionId
                )

               Subscription.deleteSubscriptionsForDiscussion(
                    discussionId = discussionId
                )

            } finally {
                session.close()
            }
        }

        fun censureTitle(discussion: Discussion) {
            //updateWithSession(discussion.censureTitle())
        }

        fun censureComment(discussion: Discussion, trackNumber: Int) {
            //discussion.censureComment(trackNumber = trackNumber)
            //update(discussion)
        }

        fun censureUser(discussion: Discussion, trackNumber: Int) {
            //discussion.censureUser(trackNumber = trackNumber)
            //update(discussion)
            //censureComment(discussion = discussion, trackNumber = trackNumber)
        }

        fun displayComment(discussion: Discussion, trackNumber: Int, model: Model) {
            discussion.displaySingleComment(model = model, trackNumber = trackNumber)
        }

        fun buildMediaConvertHandler(discussionId: DiscussionId, trackNumber: Int) : AsyncHandler<CreateJobRequest, CreateJobResult> {
            return Comment.buildMediaConvertHandler(
                discussionId = discussionId,
                trackNumber  = trackNumber
            )
        }

        fun conversionComplete(jobId: String) {
            Comment.conversionComplete(jobId)
        }
        private fun bump(discussionId: DiscussionId, country: Country) {
            Comment.bump(discussionId = discussionId, country = country)
        }

        fun getSession() : Session {
            return entityManagerFactory.
            createEntityManager().
            unwrap(Session::class.java).
            sessionFactory.
            openSession()
        }

        private fun getUrl(discussionId: DiscussionId, language: Language): String {
            return "/d/"+ discussionId+"/l/"+language.code
        }

        private fun getEnglishUrl(discussionId: DiscussionId) : String {
            return "/d/$discussionId/l/en"
        }

        /***** Tests *******************************************/

        fun testVerifyTitleContains(dto: Any, title: String) : Boolean {
            val discussionDTO: DiscussionDTO = dto as DiscussionDTO
            return discussionDTO.title.value.contains(title)
        }

        fun testVerifyDiscussionDtoId(dto: Any, discussionId: DiscussionId) {
            val discussionDTO: DiscussionDTO = dto as DiscussionDTO
            assertEquals(discussionId, discussionDTO.id)
        }

        fun testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User, numDiscussions: Int, defaultCountry: Country) {
            Subscription.testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(userA = userA, userB = userB, numDiscussions = numDiscussions, defaultCountry = defaultCountry)
        }

        fun testPaginateInbox(user: User, totalItems: Int, itemsPerPage: Int) {
            Subscription.testPaginateInbox(user = user, totalItems = totalItems, itemsPerPage = itemsPerPage)
        }

        fun testCanToggleInboxSubscriptions(defaultCountry: Country) {
            Subscription.testCanToggleInboxSubscriptions(defaultCountry = defaultCountry)
        }

        fun testVerifyInboxTotal(userId: UserId, inputValue: Int) {
            Subscription.testVerifyInboxTotal(userId = userId, inputValue = inputValue)
        }
        fun testVerifyInboxTotal(total: Int) {
            Subscription.testVerifyInboxTotal(total = total)
        }

        fun testDiscussionInboxIntegration(defaultCountry: Country) {
            Subscription.testDiscussionInboxIntegration(defaultCountry)
        }

        /******* End of companion object tests *****************/
    }

    @Embeddable
    open class DiscussionId : Serializable {
        @Column(name="discussion_id", insertable = true, updatable = false)
        var discussionId: Long

        constructor(discussionId: Long) {
            this.discussionId = discussionId
        }

        override fun hashCode(): Int {
            return discussionId.hashCode()
        }
        override fun equals(other: Any?): Boolean {
            return discussionId == (other as DiscussionId).discussionId
        }

        override fun toString(): String {
            return "$discussionId"
        }
    }

    @Entity(name="Subscription")
    private class Subscription : StandardDomainObject {
        companion object {
            private val subscriptionsByUser: MutableMap<UserId, MutableSet<Subscription>> = ConcurrentHashMap()
            private val usersByDiscussion: MutableMap<DiscussionId, MutableSet<UserId>>   = ConcurrentHashMap()
            private val locks: MutableMap<String, Lock>                                   = ConcurrentHashMap()
            fun init() {
                for(s: Subscription in genericRepository.findAll(targetClass = Subscription::class.java)){
                    subscriptionsByUser.computeIfAbsent(s.userId) { ConcurrentHashMap.newKeySet() }.add(s)
                    usersByDiscussion.computeIfAbsent(s.discussionId) { ConcurrentHashMap.newKeySet() }.add(s.userId)
                    locks["${s.discussionId}${s.userId}"] = ReentrantLock(true)
                }
            }

            fun deleteSubscriptionsForDiscussion(discussionId: DiscussionId) {
                val users: MutableSet<UserId> = usersByDiscussion[discussionId] ?: return
                for(u in users) {
                    val subscription: Subscription? = subscriptionsByUser[u]?.find { it.discussionId == discussionId}
                    if (subscription != null) {
                        subscriptionsByUser[u]?.remove(subscription)
                    }
                }
                usersByDiscussion.remove(discussionId)
            }

            fun subscribe(discussionId: DiscussionId, userId: UserId) {
                val discussion: Discussion = get(discussionId) ?: return
                val lock: Lock = getLockForSubscription(discussionId = discussionId, userId = userId)
                lock.lock()
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    replaceSubscription(
                        session           = session,
                        discussionId      = discussionId,
                        userId            = userId,
                        latestTrackNumber = discussion.getLatestTrackNumber(),
                    )
                    tx.commit()

                } finally {
                    session.close()
                    lock.unlock()
                }
            }

            fun replaceSubscription(session: Session, discussionId: DiscussionId, userId: UserId, latestTrackNumber: Int) {
                var oldSubscription: Subscription? =
                    subscriptionsByUser[userId]?.find { it.discussionId == discussionId }
                if (oldSubscription != null) {
                    session.delete(session.merge(oldSubscription))
                }
                val subscription: Subscription = session.merge(
                    Subscription(
                        discussionId    = discussionId,
                        userId          = userId,
                        lastTrackNumber = latestTrackNumber
                    )
                ) as Subscription
                //session.flush()

                if (oldSubscription != null) {
                    subscriptionsByUser[userId]!!.remove(oldSubscription)
                }

                usersByDiscussion.computeIfAbsent(discussionId) { ConcurrentHashMap.newKeySet() }.add(userId)
                subscriptionsByUser.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(subscription)
            }

            fun leaveConversation(discussionId: DiscussionId, userId: UserId) {
                val lock: Lock = getLockForSubscription(discussionId = discussionId, userId = userId)
                lock.lock()
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    val subscription: Subscription = subscriptionsByUser[userId]?.find{it.discussionId == discussionId} ?: return
                    session.delete(session.merge(subscription))

                    subscriptionsByUser[userId]?.remove(subscription)
                    usersByDiscussion[discussionId]?.remove(userId)
                    tx.commit()

                } finally {
                    session.close()
                    lock.unlock()
                }
            }

            fun notifyApplicableUsers(discussionId: DiscussionId) {
                val discussion: Discussion = get(discussionId)!!
                val users: MutableSet<UserId> = usersByDiscussion[discussionId] ?: return

                for (u in users) {
                    val subscription: Subscription = subscriptionsByUser[u]?.find {it.discussionId == discussionId} ?: continue
                    if (discussion.getLatestTrackNumber() - subscription.lastTrackNumber == 1) {
                       User.sendDiscussionNotificationEmailIfUserShouldBeEmailed(userId = u)
                    }
                }
            }

            fun resetUnreadConversationIndicator(userId: UserId,discussionId: DiscussionId) {
                val lock: Lock = getLockForSubscription(discussionId = discussionId, userId = userId)
                lock.lock()
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    replaceSubscription(
                        session           = session,
                        discussionId      = discussionId,
                        userId            = userId,
                        latestTrackNumber = get(discussionId)!!.getLatestTrackNumber()
                    )
                    tx.commit()
                } finally {
                    session.close()
                    lock.unlock()
                }
            }

            private fun getInboxPage(userId: UserId, offsetKey: Int?, itemsPerPage: Int) : List<InboxItemDTO> {
                val subscriptions: Set<Subscription> = subscriptionsByUser[userId] ?: return emptyList()

                val start = offsetKey?: 0
                var end   = start + itemsPerPage
                if (end > subscriptions.size) {
                    end = subscriptions.size
                }

                return subscriptions.map {
                    val discussion: Discussion = get(it.discussionId)!!
                    val discussionCount = discussion.getLatestTrackNumber() - it.lastTrackNumber
                        InboxItemDTO(
                        discussionId      = it.discussionId,
                        title             = discussion.title,
                        trackNumber       = discussion.getLatestTrackNumber(),
                        count             = discussionCount,
                        creationTimestamp = Comment.getLastUpdateTimestamp(discussionId = it.discussionId),
                        unread            = discussionCount > 0
                    )
                }.sortedByDescending { it.creationTimestamp }.subList(fromIndex = start, toIndex = end)
            }

            fun showInboxPage(userId: UserId, model: Model, offset: Int?, itemsPerPage: Int) {
                val inboxItems: List<InboxItemDTO> = getInboxPage(userId = userId, offsetKey = offset, itemsPerPage = itemsPerPage)

                val offsetN    = offset?: 0
                val prev       = if (offsetN - 1 < 0) { null } else { offsetN - itemsPerPage }
                var next:Int?  = offsetN + itemsPerPage
                if (inboxItems.size < itemsPerPage) { //TODO: This can be changed so you don't need the useless N-1 page.
                    next = null
                }

                println(
                    "showInboxPage -> " +
                            "inboxItemsSize: ${inboxItems.size}, " +
                            "InboxItemsPerPage: ${itemsPerPage}, " +
                            "nextOffsetKey: $next, " +
                            "previousOffsetKey: $prev"
                )

                model["inbox"] = inboxItems
                WebUtil.safeSetModelAttribute(model,"nextOffsetKey", next)
                WebUtil.safeSetModelAttribute(model,"previousOffsetKey", prev)
            }

            fun showInboxTotal(userId: UserId, httpServletResponse: HttpServletResponse) {
                httpServletResponse.writer.print(getInboxTotal(userId))
            }

            fun setInboxTotalInSession(userId: UserId, httpSession: HttpSession) {
                httpSession.setAttribute("inboxTotal", getInboxTotal(userId))
            }

            private fun getInboxTotal(userId: UserId) : Int {
                val subscriptions = subscriptionsByUser[userId] ?: return 0

                var total = 0
                subscriptions.forEach {
                    val d: Discussion? = get(it.discussionId)
                    if (d != null) {
                        total += (d.getLatestTrackNumber() - it.lastTrackNumber)
                    }
                }
                return total
            }

            /*** Tests ***************************************************/
            fun testVerifyInboxTotal(userId: UserId, inputValue: Int)  {
                assertEquals(inputValue, getInboxTotal(userId))
            }

            fun testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User, numDiscussions: Int, defaultCountry: Country) {
                val inboxItemsPerPage = 11
                val discussions: MutableList<Discussion> = mutableListOf()

                for(i in 0 until numDiscussions) {
                    discussions.add(
                        create(
                            userId          = userA.id,
                            title           = Title("Why are raw oysters so expensive? $i"),
                            duration        = 20,
                            fileName        = "test.mp3",
                            country         = defaultCountry,
                            eventSequenceId = "00000000000"
                        )
                    )
                }
                for (i in 0 until numDiscussions) {
                    reply(
                        userId          = userB.id,
                        discussionId    = discussions[i].id,
                        duration        = 30,
                        fileName        = "test.mp3",
                        eventSequenceId = "000000000000"
                    )
                }

                Thread.sleep(1000)
                assertEquals(numDiscussions, getInboxTotal(userId = userA.id))

                val model: Model = ExtendedModelMap()
                showInboxPage(userId = userA.id, model = model, offset = null, itemsPerPage = inboxItemsPerPage)

                /** Extract the model data for testing. Has to be in this class or encapsulation is broken **/
                val userAInboxPageValues: List<InboxItemDTO> = getInboxListFromModel(model)!!
                val inboxItemA = userAInboxPageValues[0]

                /** Respond directly to the discussions from the inbox and not implicitly from a list of discussion ids held in the test **/
                lateinit var lastDiscussionId: DiscussionId

                userAInboxPageValues.forEach { x ->
                    lastDiscussionId = x.discussionId
                    reply(
                        userId          = userA.id,
                        discussionId    = x.discussionId,
                        duration        = 30,
                        fileName        = "test.mp3",
                        eventSequenceId = "000000000000"
                    )
                }

                Thread.sleep(1000)

                //TODO: Move this into it's own test
                /** Verify the discussion stack is updated/bumped correctly so the last reply discussion is first in the inbox
                 *  **/
                val pageResult = getNextPage(country = defaultCountry, offsetKey = null).values
                Assertions.assertTrue(pageResult.isNotEmpty())
                testVerifyDiscussionDtoId(dto = pageResult[0], discussionId = lastDiscussionId)

                /** Verify User A and B's inbox total is updated correctly **/
                assertEquals(0, getInboxTotal(userId = userA.id))
                assertEquals(numDiscussions, getInboxTotal(userId = userB.id))

                /**Verify inbox item info matches in both inboxes for the same conversation.
                 * The top item[0] that userA replies to first will appear at the bottom of user B's inbox
                 * **/
                val inboxItemB: InboxItemDTO = getInboxPage(
                    userId       = userB.id,
                    offsetKey    = null,
                    itemsPerPage = inboxItemsPerPage
                )[numDiscussions - 1]

                assertEquals(2, inboxItemA.trackNumber)
                assertEquals(3, inboxItemB.trackNumber)
                assertEquals(inboxItemA.discussionId, inboxItemB.discussionId)
                assertEquals(inboxItemA.count,inboxItemB.count)
                Assertions.assertNotEquals(0, inboxItemB.count)
            }

            fun testPaginateInbox(user: User, totalItems: Int, itemsPerPage: Int) {
                println("testPaginateInbox -> user: ${user.id}, totalItems: $totalItems, itemsPerPage: $itemsPerPage")

                /** From left to right **/
                var p = 0
                val numOfPages: Int = (totalItems / itemsPerPage) + (totalItems % itemsPerPage)
                var offsetKey: Int? = null

                var nextOffsetKey: Int?
                var previousOffsetKey: Int? = null

                var model: Model
                println("Number of pages: $numOfPages")
                while(p < numOfPages) {
                    println("")
                    println("Page($p): Seeking inbox page forward....")
                    model = ExtendedModelMap()
                    showInboxPage(userId = user.id, model = model, offset =  offsetKey, itemsPerPage = itemsPerPage)
                    val inboxPageValues: List<InboxItemDTO> = getInboxListFromModel(model)!!
                    println("InboxPageValues Size: " + inboxPageValues.size)
                    for(i in inboxPageValues) {
                        println("InboxItem: ${i.discussionId}, count: ${i.count}, title: ${i.title}")
                    }
                    println("")


                    nextOffsetKey     = model.getAttribute("nextOffsetKey") as Int?
                    previousOffsetKey = model.getAttribute("previousOffsetKey") as Int?

                    if(p < numOfPages - 1) {
                        if(nextOffsetKey == null) {
                            println("PageNumber: $p, NumPages: $numOfPages, inboxItems: ${inboxPageValues.size}")
                            Assertions.assertNotNull(nextOffsetKey)
                        }
                        Assertions.assertNotNull(nextOffsetKey)
                    }
                    if (p > 0) {
                        Assertions.assertNotNull(previousOffsetKey)
                    }

                    println("TITLE(page $p): " + inboxPageValues[0].title)
                    /** Just verify the first element added is last. **/
                    if (p == numOfPages - 1) {
                        Assertions.assertTrue(inboxPageValues[0].title.value.contains("0"))
                    }

                    offsetKey = nextOffsetKey
                    p++

                    println("OFFSET: ${offsetKey}")
                }

                println("")

                /** From right to left **/
                /** Note - The previous function is exclusive to the starting offset whereas next is inclusive **/
                var x = 0
                while(previousOffsetKey != null) {
                    println("")
                    println("Seeking inbox page backward....")
                    model = ExtendedModelMap()
                    showInboxPage(userId = user.id, model = model, offset =  previousOffsetKey, itemsPerPage = itemsPerPage)

                    val inboxPageValues: List<InboxItemDTO> = getInboxListFromModel(model)!!
                    nextOffsetKey     = model.getAttribute("nextOffsetKey") as Int?
                    previousOffsetKey = model.getAttribute("previousOffsetKey") as Int?

                    for(i in inboxPageValues) {
                        println("InboxItem: ${i.discussionId}, count: ${i.count}, title: ${i.title}")
                    }

                    if (x > 0) {
                        Assertions.assertNotNull(nextOffsetKey)
                    }
                    if (x < numOfPages - 2) {
                        Assertions.assertNotNull(previousOffsetKey)
                    }

                    /** Just verify the first element added is last. **/
                    if (x == numOfPages - 2) {
                        println("LastPage Title: " + inboxPageValues[0].title+", (totalItems - 1): ${totalItems - 1}, x: $x, (numOfPages -2): ${numOfPages - 2}, numOfPages: $numOfPages")
                        Assertions.assertTrue(inboxPageValues[0].title.value.contains("${totalItems - 1}"))
                    }
                    x++
                }
                assertEquals(numOfPages - 1, x)
            }

            fun testCanToggleInboxSubscriptions(defaultCountry: Country) {
                val itemsPerPage = 2

                var userA = User.createNewUser()
                var discussion: Discussion = create(
                    userId          = userA.id,
                    title           = Title("Why are raw oysters so expensive?"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = "0000000000"
                )

                /** Confirm UserA has no inbox record for the discussion they created **/
                Thread.sleep(1000)
                userA = User.findById(userId = userA.id)!!
                val previousInboxTotal = getInboxTotal(userId = userA.id)
                assertEquals(0, previousInboxTotal)


                /** A random user replies to A. Ensure A has a registered inbox record **/
                discussion = reply(
                    userId          = User.createNewUser().id,
                    discussionId    = discussion.id,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "000000000"
                )
                Thread.sleep(1000)
                userA = User.findById(userId = userA.id)!!

                val currentInboxTotal = getInboxTotal(userId = userA.id)
                var model: Model = ExtendedModelMap()
                showInboxPage(userId = userA.id, model = model, offset = null, itemsPerPage = itemsPerPage)
                var inboxItem1: InboxItemDTO = getInboxItemFromModel(model)!!
                assertEquals(discussion.id, inboxItem1.discussionId)

                assertEquals(1, currentInboxTotal)
                assertEquals(1, inboxItem1.count)

                /*** User A leaves the conversation so should have no remaing records **/
                Discussion.leaveConversation(
                    userId       = userA.id,
                    discussionId = inboxItem1.discussionId
                )
                discussion = get(discussionId = inboxItem1.discussionId)!!

                /** Confirm after userA leaves they do not receive any new inbox records for this discussion **/
                reply(
                    userId          = User.createNewUser().id,
                    discussionId    = discussion.id,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "000000000"
                )
                Thread.sleep(1000)
                userA = User.findById(userId = userA.id)!!
                assertEquals(0, getInboxTotal(userId = userA.id)) //Leaving a conversation removes it from all calculation at the time of this writing

                /** Now resubscribe A, have user C send out a notification and confirm A's inbox is modified accordingly **/
                Discussion.subscribe(
                    userId       = userA.id,
                    discussionId = discussion.id
                )
                reply(
                    userId          = User.createNewUser().id,
                    discussionId    = discussion.id,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = "00000000"
                )
                Thread.sleep(1000)
                userA = User.findById(userId = userA.id)!!
                model = ExtendedModelMap()
                showInboxPage(userId = userA.id, model = model, offset = null, itemsPerPage = itemsPerPage)
                inboxItem1 = getInboxItemFromModel(model)!!

                assertEquals(1, getInboxTotal(userId = userA.id))
                assertEquals(1, inboxItem1.count)
            }

            fun testVerifyInboxTotal(total: Int) {
                var count = 0
                for (u: User in User.findAll()) {
                    count += getInboxTotal(userId = u.id)
                    //println("Total for user: ${u.id} is $count")
                }
                assertEquals(total, count)
            }

            fun testDiscussionInboxIntegration(defaultCountry: Country) {
                val userA = User.createNewUser()
                val userB = User.createNewUser()
                val numDiscussions = 7
                val discussions: MutableList<Discussion> = mutableListOf()
                val eventSequenceId = "00000"

                for(i in 0 until numDiscussions) {
                    discussions.add(
                        create(
                            userId          = userA.id,
                            title           = Title("Why are raw oysters so expensive? $i"),
                            duration        = 20,
                            fileName        = "test.mp3",
                            country         = defaultCountry,
                            eventSequenceId = eventSequenceId
                        )
                    )
                }
                val updatedDiscussions = mutableListOf<Discussion>()
                for (i in 0 until discussions.size) {
                    updatedDiscussions.add(
                        reply(
                            userId          = userB.id,
                            discussionId    = discussions[i].id,
                            duration        = 30,
                            fileName        = "test.mp3",
                            eventSequenceId = eventSequenceId
                        )
                    )
                }

                //Thread.sleep(7000)

                /** Assert that the inbox of the recipient is augmented but not inbox of the sender. **/
                println("UserA(${userA.id}): " + getInboxTotal(userA.id))
                println("UserB(${userB.id}): " + getInboxTotal(userB.id))
                assertEquals(numDiscussions, getInboxTotal(userId = userA.id))
                assertEquals(    0, getInboxTotal(userId = userB.id))

                for (i in 0 until updatedDiscussions.size) {
                    reply(
                        userId          = userA.id,
                        discussionId    = updatedDiscussions[i].id,
                        duration        = 30,
                        fileName        = "test.mp3",
                        eventSequenceId = eventSequenceId
                    )
                }

                //Thread.sleep(7000)

                /** Assert that replying clears the senders inbox but will augment the inbox of the recipient. **/
                println("UserA(${userA.id}): " + getInboxTotal(userA.id))
                println("UserB(${userB.id}): " + getInboxTotal(userB.id))
                assertEquals(    0, getInboxTotal(userId = userA.id))
                assertEquals(numDiscussions, getInboxTotal(userId = userB.id))
            }

            private fun getInboxListFromModel(model: Model) : List<InboxItemDTO>? {
                return (model.getAttribute("inbox") as List<InboxItemDTO>?)
            }
            private fun getInboxItemFromModel(model: Model) : InboxItemDTO? {
                val list = getInboxListFromModel(model) ?: return null
                if(list.isNotEmpty()) {
                    return list[0]
                }
                return null
            }

            private fun getLockForSubscription(discussionId: DiscussionId, userId: UserId) : Lock {
                val lock = locks["$discussionId$userId"]
                if(lock != null) return lock

                locks["$discussionId$userId"] = ReentrantLock(true)
                return locks["$discussionId$userId"]!!
            }
        }

        private class InboxItemDTO (
            val discussionId: DiscussionId,
            val title: Title,
            val trackNumber: Int,
            val count: Int,
            val creationTimestamp: Date,
            val unread: Boolean,
        )

        @Embedded
        private val discussionId: DiscussionId
        @Embedded
        private val userId: UserId
        private val lastTrackNumber: Int

        constructor(discussionId: DiscussionId, userId: UserId, lastTrackNumber: Int) {
            this.discussionId    = discussionId
            this.userId          = userId
            this.lastTrackNumber = lastTrackNumber
        }
        override fun equals(other: Any?): Boolean {
            val them = other as Subscription
            return (them.discussionId == this.discussionId) && (them.userId == this.userId)
        }

        override fun hashCode(): Int {
            return "${discussionId}$userId".hashCode()
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private var realId: Long = -1

    var id: DiscussionId
        get(): DiscussionId {
            return DiscussionId(this.realId)
        }
        set(x) {
            this.realId = x.discussionId
        }

    @Embedded
    private val title: Title

    @Embedded
    private val youtubeLink: YoutubeLink?
    private val creationTimestamp: Date

    @Embedded
    private val userId: UserId

    @ManyToOne(optional = true)
    @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false, updatable = false, insertable = true)
    private val country: Country

    @Version
    val version = 0

    constructor(
        userId: UserId,
        title: Title,
        youtubeLink: YoutubeLink? = null,
        country: Country
    ) {
        this.userId            = userId
        this.title             = title
        this.creationTimestamp = Date()
        this.youtubeLink       = youtubeLink
        this.country           = country

        println("Creating discussion ($title) for user: ($userId)")
    }

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    /*val audioUrl:  String
        get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
            needsConversion = this.needsConversion,
            fileName        = this.fileName
        ) } else {
            EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
        }*/

    //TODO: Some annotations needed to highlight that this tricky looking thing does indeed have unit tests
    /** This is used dynamically in a .html template. Ignore the gray (no usages) **/
    /*fun getUrl(language: Language): String {
        //return "/d/" + this.id + "/l/" + language.code
        return Discussion.getUrl(discussionId = this.id, language = language)
    }*/

    private fun getEnglishUrl() : String {
        //return "/d/" + this.id + "/l/en"
        return getEnglishUrl(discussionId = this.id)
    }

    fun goToDiscussionPageWithBaseUrl(webClient: WebClient, url: String): HtmlPage {
        return webClient.getPage(url + this.getEnglishUrl())
    }

    /*fun censureTitle() : Discussion {
        this.title = Title("   ")
        return this
    }*/
    fun censureComment(trackNumber: Int) {
        //if (trackNumber > Comment.getComments(discussionId = this.id).size) return
        //val comment: Comment = Comment.getComments(discussionId = this.id)[trackNumber - 1]
        //comment.censured = true
    }

    /*fun censureUser(trackNumber: Int) {
        if (trackNumber > Comment.getComments(discussionId = this.id).size) return

        censureComment(trackNumber = trackNumber)

        User.findById(Comment.getComments(discussionId = this.id)[trackNumber - 1].userId)?.censureUser()
    }*/

    fun isLastUserToComment(userId: UserId) : Boolean {
        return Comment.isLastUserToComment(discussionId = this.id, userId = userId)

    }

    fun displaySingleComment(model: Model, trackNumber: Int) {
        if (trackNumber >= Comment.getComments(discussionId = this.id).size) return
        model["comment"] = Comment.getComments(discussionId = this.id)[trackNumber - 1]
    }

    fun getCommentsView(start: Int, end: Int) : Any {
        val comments = Comment.getComments(discussionId = this.id)

        val normalizedEnd: Int = if (end > comments.size) { comments.size} else { end }
        val temp: MutableList<Comment> = mutableListOf()
        for(c in comments.subList(start, normalizedEnd)) {
            temp.add(c)
        }
        return temp
    }

    private fun getLatestTrackNumber() : Int {
        return Comment.getComments(discussionId = this.id).size
    }

    private fun getNumberOfReplies() : Int {
        return Comment.getComments(discussionId = this.id).size - 1
    }

    @Entity(name="Comment")
    @Table(name = "comment")
    private class Comment : StandardDomainObject {
        private val creationTimestamp: Date
        private val fileName: String
        @ManyToOne(optional = true)
        @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false, updatable = false, insertable = true)
        private val country: Country
        private val trackNumber: Int
        @Embedded
        private val discussionId: DiscussionId
        @Embedded
        private val userId: UserId?
        private val duration: Int
        private var needsConversion: Boolean
        private var audioConversionInProgress = false
        private var conversionJobId: String? = null
        private var censured: Boolean = false
        @Version
        private val version = 0

        constructor(
            discussionId: DiscussionId,
            country: Country,
            userId: UserId,
            fileName: String,
            duration: Int,
            needsConversion: Boolean,
            trackNumber: Int
        ) {
            this.discussionId      = discussionId
            this.country           = country
            this.userId            = userId
            this.fileName          = fileName
            this.trackNumber       = trackNumber
            this.duration          = duration
            this.needsConversion   = needsConversion
            this.creationTimestamp = Date()
        }

        private val audioUrl: String
            get() = if (!censured) { DiscussionUtility.getPotentiallyConvertedFileLocation(
                needsConversion = this.needsConversion,
                fileName        = this.fileName
            ) } else {
                EnvironmentConfigs.mscCdn + "/sounds/horse.mp3"
            }

        companion object {
            private val commentsByDiscussion: MutableMap<DiscussionId, MutableList<Comment>>     = ConcurrentHashMap<DiscussionId, MutableList<Comment>>()
            private val allCommentsSorted: MutableMap<Country, BumpStack<DiscussionId, Comment>> = ConcurrentHashMap()
            private val conversionJobInfo: MutableMap<String, Long>                              = ConcurrentHashMap()
            private val allComments: MutableMap<Long, Comment>                                   = ConcurrentHashMap()
            fun init() {
                val results: List<Comment> = EntityManagementUtils.
                getSession(entityManagerFactory).
                createQuery("SELECT c FROM Comment c ORDER BY c.discussionId, c.trackNumber ASC").resultList as List<Comment>

                for(result: Comment in results) {
                    val c: Comment = updateCache(updatedComment = result)
                    if (c.needsConversion && (c.conversionJobId != null)) {
                        conversionJobInfo[c.conversionJobId!!] = c.id
                    }
                }
            }

            fun bump(discussionId: DiscussionId, country: Country) {
                allCommentsSorted[country]?.bump(key = discussionId)
            }

            fun getComments(discussionId: DiscussionId) : List<Comment> {
                return commentsByDiscussion[discussionId]!!
            }

            fun getLastUpdateTimestamp(discussionId: DiscussionId) : Date {
                val comments: List<Comment> = commentsByDiscussion[discussionId]!!
                return comments[comments.size -1].creationTimestamp
            }

            fun getForDisplay(discussionId: DiscussionId, title: Title, numberOfReplies: Int): DiscussionDTO? {
                val comments: List<Comment> = getComments(discussionId = discussionId)
                val lastComment: Comment = comments[comments.size - 1]

                return DiscussionDTO(
                    id              = discussionId,
                    title           = title,
                    numberOfReplies = numberOfReplies,
                    updateTimestamp = lastComment.creationTimestamp,
                    needsConversion = lastComment.needsConversion,
                    fileName        = lastComment.fileName,
                    censured        = lastComment.censured
                )
            }

            fun save(comment: Comment) : Comment {
                val session: Session = getSession()
                val tx = session.beginTransaction()
                try {
                    val updatedComment: Comment = session.merge(comment) as Comment
                    tx.commit()
                    return updateCache(updatedComment)

                } finally {
                    session.close()
                }
            }

            fun saveWithSession(session: Session, comment: Comment) : Comment {
                val updatedComment: Comment = session.merge(comment) as Comment
                session.flush()

                return updateCache(updatedComment)
            }

            private fun updateCache(updatedComment: Comment) : Comment {
                val stack: BumpStack<DiscussionId, Comment> = allCommentsSorted[updatedComment.country] ?: BumpStack()
                if (!allCommentsSorted.contains(updatedComment.country)) {
                    allCommentsSorted[updatedComment.country] = stack
                }

                if(stack.get(key = updatedComment.discussionId) != null) {
                    stack.update(
                        key   = updatedComment.discussionId,
                        value = updatedComment
                    )
                } else {
                    stack.push(
                        key     = updatedComment.discussionId,
                        element = updatedComment
                    )
                }

                allComments[updatedComment.id] = updatedComment

                val comments: MutableList<Comment> = commentsByDiscussion[updatedComment.discussionId] ?: mutableListOf()
                if (updatedComment.trackNumber <= comments.size) {
                    comments[updatedComment.trackNumber - 1] = updatedComment
                } else {
                    comments.add(updatedComment)
                }
                commentsByDiscussion[updatedComment.discussionId] = comments
                return updatedComment
            }

            fun conversionComplete(jobId: String) {
                val comment: Comment              = allComments[getCommentIdByJobId(jobId = jobId)] ?: return
                comment.needsConversion           = false
                comment.audioConversionInProgress = false
                save(comment)
            }

            private fun getCommentIdByJobId(jobId: String) : Long? {
                return conversionJobInfo[jobId]
            }

            fun buildMediaConvertHandler(discussionId: DiscussionId, trackNumber: Int) : AsyncHandler<CreateJobRequest, CreateJobResult> {
                return MediaConvertHandler(discussionId = discussionId, trackNumber = trackNumber)
            }
            private class MediaConvertHandler : AsyncHandler<CreateJobRequest, CreateJobResult> {
                private val discussionId: DiscussionId
                private val trackNumber: Int

                constructor(discussionId: DiscussionId, trackNumber: Int) {
                    this.discussionId = discussionId
                    this.trackNumber  = trackNumber
                }
                override fun onError(exception: Exception) {
                    loggingService.error("Error sending request to AWS Media convert", exception)
                }
                override fun onSuccess(request: CreateJobRequest, result: CreateJobResult) {
                    val comment: Comment = commentsByDiscussion[discussionId]!![trackNumber]
                    comment.conversionJobId           = result.job.id
                    comment.audioConversionInProgress = true
                    save(comment)
                }
            }

            fun convertCommentIfNeededAsync(
                comment: Comment,
                eventSequenceId: String,
                mediaConversionService: AudioConversionRequestService
            ) {
                if (!comment.needsConversion) return

                UserEvent(
                    name               = "conversion_request",
                    methodName         = "convertLastCommentIfNeededAsync",
                    context            = "recording",
                    userId             = comment.userId,
                    sequenceId         = eventSequenceId,
                    sequenceComplete   = false,
                ).asyncSave()

                mediaConversionService.startConvertingAudioFile(
                    discussionId  = comment.discussionId,
                    trackNumber   = comment.trackNumber,
                    fileName      = comment.fileName
                )
            }

            fun getNextPage(country: Country, offSetKey: DiscussionId?): BumpStack.Page<DiscussionId, Any> {
                val page = allCommentsSorted[country]?.nextPage(
                    inputKey = offSetKey,
                    N        = applicationProperties.discussionsPerPage
                ) ?: return BumpStack.Page(null, null, listOf())

                return buildPageForDTO(page)
            }

            fun getPreviousPage(country: Country, offSetKey: DiscussionId) : BumpStack.Page<DiscussionId, Any> {
                val page = allCommentsSorted[country]?.previousPage(
                    inputKey = offSetKey,
                    N        = applicationProperties.discussionsPerPage
                ) ?: return BumpStack.Page(null, null, listOf())

                return buildPageForDTO(page)
            }

            private fun buildPageForDTO(page: BumpStack.Page<DiscussionId, Comment>) : BumpStack.Page<DiscussionId, Any> {
                return BumpStack.Page(
                    previousOffsetKey = page.previousOffsetKey,
                    nextOffsetKey     = page.nextOffsetKey,
                    values            = page.values.map {
                        val discussion: Discussion = get(discussionId = it.discussionId)!!
                        DiscussionDTO(
                            id              = it.discussionId,
                            needsConversion = it.needsConversion,
                            censured        = it.censured,
                            title           = discussion.title,
                            fileName        = it.fileName,
                            numberOfReplies = discussion.getNumberOfReplies(),
                            updateTimestamp = getLastUpdateTimestamp(discussionId = it.discussionId)
                        )
                    }
                )
            }

            fun delete(discussionId: DiscussionId, country: Country) {
                val comments: List<Comment>? = commentsByDiscussion[discussionId]
                if (comments != null) {
                    for (c in comments) {
                        allComments.remove(c.id)
                        if (c.conversionJobId != null) {
                            conversionJobInfo.remove(c.conversionJobId)
                        }
                    }
                }

                commentsByDiscussion.remove(discussionId)
                allCommentsSorted[country]?.remove(discussionId)
            }

            fun isLastUserToComment(discussionId: DiscussionId, userId: UserId) : Boolean {
                val comments = getComments(discussionId = discussionId)
                return comments[comments.size - 1].userId == userId
            }
        }

    }

    fun testVerifyTrackNumber(potentialTrackNumber: Int) {
        assertEquals(potentialTrackNumber, this.getLatestTrackNumber())
    }
    fun testVerifyReplyCount(potentialCount: Int) {
        assertEquals(potentialCount, Comment.getComments(discussionId = this.id).size - 1)
    }

    fun testVerifyTitle(title: String) : Boolean {
        return this.title.value == title
    }

    override fun equals(other: Any?) : Boolean {
        if (other == null) {
            return false
        }
        return this.id == (other as Discussion).id
    }

    override fun hashCode() : Int {
        return id.hashCode()
    }
}