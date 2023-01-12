package com.bloip.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Country
import com.bloip.domain.user.User
import com.bloip.integration.mocks.MockMediaConversionService
import com.bloip.integration.utils.TestUtils
import com.bloip.services.*
import com.bloip.services.localization.CountryService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.lang.reflect.Field

/**
 * Created by Usman Mutawakil on 9/8/22.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class InboxScenariosIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val discussionService: DiscussionService,
    @Autowired val countryService: CountryService
)
{
    private lateinit var s3: AmazonS3

    private var numDiscussions               = 7
    lateinit var defaultCountry: Country
    private var eventSequenceId: String = "XXXXXXXXX"

    private fun clearEmailBucket() {
        val objectList = s3.listObjects(applicationProperties.emailBucket)
        for(s in objectList.objectSummaries) {
            s3.deleteObject(applicationProperties.emailBucket, s.key)
        }
    }

    private fun clearDatabaseTables() {
        User.deleteAll()
        discussionService.deleteAll()
    }

    fun createTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User,discussions: MutableList<Discussion> = mutableListOf()) {
        for(i in 0 until numDiscussions) {
            discussions.add(
                discussionService.create(
                    user            = User.findById(userId = userA.id)!!,
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId
                )
            )
        }

        for (i in 0 until discussions.size) {
            discussionService.reply(
                user            = User.findById(userId = userB.id)!!,
                discussion      = discussions[i],
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        }
    }

    fun createTwoWayConversationAcrossMultipleDiscussionsWithFullReplies(userA: User, userB: User,discussions: MutableList<Discussion> = mutableListOf()) {
        for(i in 0 until numDiscussions) {
            discussions.add(
                discussionService.create(
                    user            = User.findById(userId = userA.id)!!,
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
                discussionService.reply(
                    user            = User.findById(userId = userB.id)!!,
                    discussion      = discussions[i],
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = eventSequenceId
                )
            )
        }
        for (i in 0 until updatedDiscussions.size) {
            discussionService.reply(
                user            = User.findById(userId = userA.id)!!,
                discussion      = updatedDiscussions[i],
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        }
    }


    @BeforeAll
    fun setup() {
        println("BEFORE ALL...Going to clear various tables to ensure db is empty for these tests...")
        clearDatabaseTables()

        defaultCountry = countryService.getCanonicalByCode("us")!!
        discussionService.mediaConversionService = MockMediaConversionService()
        applicationProperties.maxDiscussionCreationsPerDay = 100

        s3 = AmazonS3ClientBuilder.standard().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        ).build()
        clearEmailBucket()
        println("BEFORE ALL COMPLETE")
    }

    @AfterAll
    fun cleanup() {
        println("Deleting all relevant tables for the completed tests...")
        clearDatabaseTables()
        println("AFTER ALL COMPLETE")
    }

    @Test
    @Order(0)
    fun verify_inboxes__exists__in__database() {
        cleanup()
        val userA = User.createNewUser()
        val userB = User.createNewUser()
        createTwoWayConversationAcrossMultipleDiscussionsWithFullReplies(userA = userA, userB = userB)
        User.testVerifyDatabaseInboxTotal(total = numDiscussions * 2)
    }

    //TODO: Remove the reflection code from this beast!!!!!
    /** Something that has made this fail in the past was poor hibernate
     * annotation mapping that resulted in aggregates not being saved consistently
     *  **/
    @Test
    @Order(1)
    fun verify__discussion__and__comment__records__match__what__is__expected() {
        cleanup()
        val discussions: MutableList<Discussion> = mutableListOf()
        createTwoWayConversationAcrossMultipleDiscussions(userA = User.createNewUser(), userB = User.createNewUser(), discussions = discussions)

        /** Remember the number of replies is 1 less than the number of comments **/
        val firstDiscussion: Discussion = discussions[0]
        val numOfRepliesField: Field = Discussion::class.java.getDeclaredField("numberOfReplies")
        numOfRepliesField.isAccessible = true
        assertEquals(1, numOfRepliesField.getInt(firstDiscussion) )

        /** Verify discussion has correct number of comments **/
        val commentsField: Field = Discussion::class.java.getDeclaredField("comments")
        commentsField.isAccessible = true
        assertEquals(2, (commentsField.get(discussions[0]) as List<*>).size)

        /** Verify the total number of replies is consistent with the number of discussions and replies attempted to be created **/
        var commentsTotal = 0
        for(d in discussions) {
            commentsTotal += (commentsField.get(d) as List<*>).size
        }
        assertEquals(numDiscussions * 2, commentsTotal)

        /** Verify the two replies are not the same (bad hibernate configuration on many-to-one one-to-many has caused this before) **/
        val comments = commentsField.get(discussions[0]) as List<*>
        val idField: Field = comments[0]!!.javaClass.superclass.getDeclaredField("id")
        idField.isAccessible = true
        assertNotEquals(idField.getLong(comments[0]), idField.getLong(comments[1]))
    }

    @Test
    @Order(2)
    fun verify__media__conversion__services__run__for__appropriate__files() {
        cleanup()
        val userA = User.createNewUser()
        val userB = User.createNewUser()
        val discussions: MutableList<Discussion> = mutableListOf()
        discussionService.mediaConversionService = MockMediaConversionService()

        /***Verify media conversion services runs on creation for certain files ***/
        for(i in 0 until numDiscussions) {
            discussionService.create(
                user            = User.findById(userId = userA.id)!!,
                title           = Title("Why are raw oysters so expensive? $i"),
                duration        = 20,
                fileName        = "test.mp3",
                country         = defaultCountry,
                eventSequenceId = eventSequenceId
            )
        }
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertEquals(numDiscussions, (discussionService.mediaConversionService as MockMediaConversionService).count)

        /*** -----------------------------**/

        /****Verify media conversion services is not running on creation for non MP4 Files ****/
        discussionService.mediaConversionService = MockMediaConversionService()

        discussions.clear()
        for (i in 0 until numDiscussions) {
            discussions.add(
                discussionService.create(
                    user            = userA,
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 20,
                    fileName        = "test.mp4",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId
                )
            )
        }
        assertFalse((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).count == 0)

        /*** -----------------------------**/

        /** Verify replies are not triggering media conversions on mp4 files **/
        discussionService.mediaConversionService = MockMediaConversionService()
        for (i in 0 until discussions.size) {
            discussions[i] = discussionService.reply(
                user            = User.findById(userId = userB.id)!!,
                discussion      = discussions[i],
                duration        = 30,
                fileName        = "test.mp4",
                eventSequenceId = eventSequenceId
            )
        }
        assertFalse((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).count == 0)

        /*** -----------------------------**/

        /** Verify replies ARE triggering media conversions on NON mp4 files **/
        discussionService.mediaConversionService = MockMediaConversionService()
        for (i in 0 until discussions.size) {
            discussionService.reply(
                user            = userA,
                discussion      = discussions[i],
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        }
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).count == discussions.size)
    }

    @Test
    @Order(3)
    fun verify__inboxes__of__two__way__conversation__across__multiple__discussions() {
        cleanup()
        User.testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(
            userA          = User.createNewUser(),
            userB          = User.createNewUser(),
            numDiscussions = numDiscussions,
            defaultCountry = defaultCountry
        )
    }

    @Test
    @Order(4)
    fun can__paginate__through__inbox() {
        cleanup()
        applicationProperties.inboxItemsPerPage  = 2
        val userA = User.createNewUser()
        val userB = User.createNewUser()

        createTwoWayConversationAcrossMultipleDiscussionsWithFullReplies(
            userA = userA,
            userB = userB
        )

        User.testPaginateInbox(
            user         = User.findById(userId = userA.id)!!,
            totalItems   = numDiscussions,
            itemsPerPage = applicationProperties.inboxItemsPerPage
        )
    }

    /** User can unsubscribe, be skipped a notification, resubscribe, then receive new notifications **/
    @Test
    @Order(5)
    fun can__toggle__inbox__subscriptions() {
        cleanup()
        User.testCanToggleInboxSubscriptions(
            defaultCountry = defaultCountry
        )
    }

    /** User A can delete an item from the inbox and still receive new notifications in that conversation **/
    @Test
    @Order(6)
    fun can__delete__inbox__items__without__unsubscribing() {
        cleanup()
        User.testDeleteInboxItemWithoutUnsubscribing(defaultCountry)
    }

    @Test
    @Order(7)
    fun can__trigger__multiple__replies__notifications() {
        cleanup()
        val numUsers = 10

        val firstUser: User = User.createNewUser()
        val users: MutableList<User> = mutableListOf()
        for(i in 0 until numUsers) {
            users.add(User.createNewUser())
        }

        val discussion = discussionService.create(
            user      = firstUser,
            title     = Title("Why are raw oysters so expensive?"),
            duration  = 30,
            fileName  = "test.mp3",
            country   =  defaultCountry,
            eventSequenceId = eventSequenceId
        )

        /** Users comment. Should create inboxes from 0 to 9
         * since they are triggering notifications to everyone as well**/
        for(i in 0 until users.size) {
            discussionService.reply(
                user            = User.findById(userId = users[i].id)!!,
                discussion      = discussionService.get(discussionId = discussion.id)!!,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        }

        /**Verify recipients inboxes
         * Should run from 1 to 10 since each user is adding 1 to each inbox in the 0 to 9 set
         * **/
        for(i in 0 until users.size) {
            User.findById(userId = users[i].id)!!.testVerifyInboxTotal(users.size - i - 1)
            println("Verifying user inbox...")
        }
    }

    /** User can unsubscribe, be skipped a notification, resubscribe, then receive new notifications **/
    @Test
    @Order(8)
    fun can__block__user__from__two__consecutive__replies() {
        cleanup()

        val userC = User.createNewUser()
        val userD = User.createNewUser()

        var discussion = discussionService.create(
            user            = userC,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         = defaultCountry,
            eventSequenceId = eventSequenceId
        )

        var exception: Exception? = null
        try {
            discussionService.reply(
                user            = User.findById(userId = userC.id)!!,
                discussion      = discussion,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        } catch(e: Exception) {
            exception = e
        }
        assertNotNull(exception, "Exception should be thrown on double posting")

        discussion = discussionService.reply(
            user            = userD,
            discussion      = discussion,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = eventSequenceId
        )

        var exceptionB: Exception? = null
        try {
            discussionService.reply(
                user            = User.findById(userId = userD.id)!!,
                discussion      = discussion,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId
            )
        } catch(e: Exception) {
            exceptionB = e
        }
        assertNotNull(exceptionB, "Exception should be thrown on double posting")
    }

    @Test
    @Order(9)
    fun replies__will__trigger__email__to__all__subscribed__users__of__a__discussion__() {
        cleanup()
        val userX = User.createNormalUser("testX1@dev.bloip.com", "XXXXXXXX")
        val userY = User.createNormalUser("testX2@dev.bloip.com", "XXXXXXXX")

        val discussion = discussionService.create(
            user            = userX,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         =  defaultCountry,
            eventSequenceId = "XXXXXXXX"
        )

        discussionService.reply(
            user            = userY,
            discussion      = discussion,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = "XXXXXXX"
        )
        /** Verify email was sent **/
        assertEquals(1, TestUtils.numEmailsPresent(s3 = s3, emailBucket = applicationProperties.emailBucket))
    }

    @Test
    @Order(10)
    fun replies__will__not__trigger__email__to__users__with__email__disabled__() {
        cleanup()
        var userX = User.createNormalUser("testXX2@dev.bloip.com", "XXXXXXXX")
        val userY = User.createNewUser()

        val discussion = discussionService.create(
            user            = userX,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         =  defaultCountry,
            eventSequenceId = "XXXXXXXX"
        )

        userX = User.findById(userId = userX.id)!!
        userX.updateNotificationStatus(disabled = true)

        discussionService.reply(
            user            = userY,
            discussion      = discussion,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = "XXXXXXX"
        )
        /** Verify email was not sent **/
        assertEquals(0, TestUtils.numEmailsPresent(s3 = s3, emailBucket = applicationProperties.emailBucket))
    }
}