package com.bloip.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.UserEvent
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Country
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User
import com.bloip.integration.utils.TestUtils
import com.bloip.services.localization.CountryService
import com.bloip.services.localization.translation.LanguageService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.ui.ExtendedModelMap

/**
 * Created by Usman Mutawakil on 9/8/22.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class InboxScenariosIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val countryService: CountryService,
    @Autowired val languageService: LanguageService
)
{
    private lateinit var s3: AmazonS3

    private  var numDiscussions          = 7
    private lateinit var defaultCountry: Country
    private lateinit var language: Language
    private  var eventSequenceId: String = "0000000000"

    private fun clearEmailBucket() {
        val objectList = s3.listObjects(applicationProperties.emailBucket)
        for(s in objectList.objectSummaries) {
            s3.deleteObject(applicationProperties.emailBucket, s.key)
        }
    }

    private fun clearDatabaseTables() {
        UserEvent.deleteAll()
        User.deleteAll()
        Discussion.deleteAll()
    }

    fun createTwoWayConversationAcrossMultipleDiscussions(userA: User, userB: User,discussions: MutableList<Discussion> = mutableListOf()) {
        for(i in 0 until numDiscussions) {
            discussions.add(
                Discussion.create(
                    userId          = userA.id,
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId,
                    language        = language
                )
            )
        }

        for (i in 0 until discussions.size) {
            Discussion.reply(
                userId          = userB.id,
                discussionId    = discussions[i].id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
        }
    }

    fun createTwoWayConversationAcrossMultipleDiscussionsWithFullReplies(
        userA: User,
        userB: User,
        discussions: MutableList<Discussion> = mutableListOf()
    ) {
        for(i in 0 until numDiscussions) {
            discussions.add(
                Discussion.create(
                    userId          = userA.id,
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 20,
                    fileName        = "test.mp3",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId,
                    language        = language
                )
            )
        }
        val updatedDiscussions = mutableListOf<Discussion>()
        for (i in 0 until discussions.size) {
            updatedDiscussions.add(
                Discussion.reply(
                    userId          = userB.id,
                    discussionId    = discussions[i].id,
                    duration        = 30,
                    fileName        = "test.mp3",
                    eventSequenceId = eventSequenceId,
                    language        = language
                )
            )
        }
        for (i in 0 until updatedDiscussions.size) {
            Discussion.reply(
                userId          = userA.id,
                discussionId    = updatedDiscussions[i].id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
        }
    }


    @BeforeAll
    fun setup() {
        clearDatabaseTables()
        language       = languageService.getCanonicalByCode(code = "en")!!
        defaultCountry = countryService.getCanonicalByCode("us")!!
        User.maxDiscussionCreationsPerDay = 100

        s3 = AmazonS3ClientBuilder.standard().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        ).build()
        clearEmailBucket()
    }

    @AfterAll
    fun cleanup() {
        clearDatabaseTables()
    }

    @Test
    @Order(0)
    fun discussion__inbox__integration() {
        clearDatabaseTables()
        Discussion.testDiscussionInboxIntegration(defaultCountry, language)
    }

     //TODO: Remove the reflection code from this beast!!!!!
    /** Something that has made this fail in the past was poor hibernate
     * annotation mapping that resulted in aggregates not being saved consistently
     *  **/
    @Test
    @Order(1)
    fun verify__discussion__and__comment__records__match__what__is__expected() {
        clearDatabaseTables()

        val discussions: MutableList<Discussion> = mutableListOf()
        createTwoWayConversationAcrossMultipleDiscussions(
            userA       = User.createNewUser(),
            userB       = User.createNewUser(),
            discussions = discussions
        )

        /** Remember the number of replies is 1 less than the number of comments **/
        val firstDiscussion: Discussion = discussions[0]
        firstDiscussion.testVerifyReplyCount(potentialCount = 1)

        /** Verify discussion has correct number of comments/tracks **/
        firstDiscussion.testVerifyTrackNumber(2)

        /** Verify the total number of replies is consistent with
         *  the number of discussions and replies attempted to be created **/
        var commentsTotal = 0
        for(d in discussions) {
            commentsTotal += 2//(commentsField.get(d) as List<*>).size
            d.testVerifyTrackNumber(potentialTrackNumber = 2)
        }
        assertEquals(numDiscussions * 2, commentsTotal)
    }

    @Test
    @Order(2)
    fun verify__media__conversion__services__run__for__appropriate__files() {
        clearDatabaseTables()
        val userA = User.createNewUser()
        val userB = User.createNewUser()
        val discussions: MutableList<Discussion> = mutableListOf()

        /***Verify media conversion services runs on creation for certain files ***/
        for(i in 0 until numDiscussions) {
            Discussion.create(
                userId          = userA.id,
                title           = Title("Why are raw oysters so expensive? $i"),
                duration        = 20,
                fileName        = "test.mp3",
                country         = defaultCountry,
                eventSequenceId = eventSequenceId,
                language        = language
            )
            //assertEquals(1, UserEvent.findByUserIdAndName(userId = userA.id, name="conversion_request").size)
            //UserEvent.clear(userId = userA.id)
        }
        assertTrue(UserEvent.numOfConversionRequests() == numDiscussions)
        UserEvent.deleteAll()
        /*** -----------------------------**/

        /****Verify media conversion services is not running on creation for non MP4 Files ****/
        discussions.clear()
        for (i in 0 until numDiscussions) {
            discussions.add(
                Discussion.create(
                    userId          = userA.id,
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 20,
                    fileName        = "test.mp4",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId,
                    language        = language
                )
            )
            //assertTrue(UserEvent.findByUserIdAndName(userId = userA.id, name = "conversion_request").isEmpty())
            //UserEvent.clear(userId = userA.id)
        }
        assertTrue(UserEvent.numOfConversionRequests() == 0)
        UserEvent.deleteAll()

        /*** -----------------------------**/

        /** Verify replies are not triggering media conversions on mp4 files **/
        for (i in 0 until discussions.size) {
            discussions[i] = Discussion.reply(
                userId            = userB.id,
                discussionId      = discussions[i].id,
                duration        = 30,
                fileName        = "test.mp4",
                eventSequenceId = eventSequenceId,
                language        = language
            )
            //assertTrue(UserEvent.findByUserIdAndName(userId = userB.id, name="conversion_request").isEmpty())
            //UserEvent.clear(userId = userB.id)
        }
        assertTrue(UserEvent.numOfConversionRequests() == 0)
        UserEvent.deleteAll()
        /*** -----------------------------**/

        /** Verify replies ARE triggering media conversions on NON mp4 files **/
        for (i in 0 until discussions.size) {
            Discussion.reply(
                userId          = userA.id,
                discussionId    = discussions[i].id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
            //assertTrue(UserEvent.findByUserIdAndName(userId = userA.id, name="conversion_request").size == 1)
            //UserEvent.clear(userId = userA.id)
        }
        assertTrue(UserEvent.numOfConversionRequests() == numDiscussions)
        UserEvent.deleteAll()
    }

    @Test
    @Order(3)
    fun verify__inboxes__of__two__way__conversation__across__multiple__discussions() {
        clearDatabaseTables()
        Discussion.testVerifyInboxesOfTwoWayConversationAcrossMultipleDiscussions(
            userA          = User.createNewUser(),
            userB          = User.createNewUser(),
            numDiscussions = numDiscussions,
            defaultCountry = defaultCountry,
            language       = language
        )
    }

    @Test
    @Order(4)
    fun can__paginate__through__inbox() {
        clearDatabaseTables()
        val userA = User.createNewUser()
        val userB = User.createNewUser()

        createTwoWayConversationAcrossMultipleDiscussionsWithFullReplies(
            userA = userA,
            userB = userB
        )
        Thread.sleep(3000)
        Discussion.testPaginateInbox(
            user         = User.findById(userId = userA.id)!!,
            totalItems   = numDiscussions,
            itemsPerPage = 2
        )
    }

    /** User can unsubscribe, be skipped a notification, resubscribe, then receive new notifications **/
    @Test
    @Order(5)
    fun can__toggle__inbox__subscriptions() {
        clearDatabaseTables()
        Discussion.testCanToggleInboxSubscriptions(
            defaultCountry = defaultCountry,
            language       = language
        )
    }

    @Test
    @Order(6)
    fun can__trigger__multiple__replies__notifications() {
        clearDatabaseTables()
        val numUsers = 10

        val firstUser: User = User.createNewUser()
        val users: MutableList<User> = mutableListOf()
        for(i in 0 until numUsers) {
            users.add(User.createNewUser())
        }

        val discussion = Discussion.create(
            userId    = firstUser.id,
            title     = Title("Why are raw oysters so expensive?"),
            duration  = 30,
            fileName  = "test.mp3",
            country   =  defaultCountry,
            eventSequenceId = eventSequenceId,
            language        = language
        )
        Thread.sleep(1000)
        /** Users comment. Should create inboxes from 0 to 9
         * since they are triggering notifications to everyone as well**/
        for(i in 0 until users.size) {
            Discussion.reply(
                userId          = users[i].id,
                discussionId    = discussion.id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
        }
        Thread.sleep(1000)
        /**Verify recipients inboxes
         * Should run from 1 to 10 since each user is adding 1 to each inbox in the 0 to 9 set
         * **/
        for(i in 0 until users.size) {
            Discussion.testVerifyInboxTotal(userId = users[i].id, inputValue = users.size - i - 1)
            println("Verifying user inbox...")
        }
    }

    /** User can unsubscribe, be skipped a notification, resubscribe, then receive new notifications **/
    @Test
    @Order(7)
    fun can__block__user__from__two__consecutive__replies() {
        clearDatabaseTables()

        val userC = User.createNewUser()
        val userD = User.createNewUser()

        var discussion = Discussion.create(
            userId          = userC.id,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         = defaultCountry,
            eventSequenceId = eventSequenceId,
            language        = language
        )
        Thread.sleep(1000)
        var exception: Exception? = null
        try {
            Discussion.reply(
                userId          = userC.id,
                discussionId    = discussion.id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
        } catch(e: Exception) {
            exception = e
        }
        assertNotNull(exception, "Exception should be thrown on double posting")

        discussion = Discussion.reply(
            userId          = userD.id,
            discussionId    = discussion.id,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = eventSequenceId,
            language        = language
        )
        Thread.sleep(1000)
        var exceptionB: Exception? = null
        try {
            Discussion.reply(
                userId          = userD.id,
                discussionId    = discussion.id,
                duration        = 30,
                fileName        = "test.mp3",
                eventSequenceId = eventSequenceId,
                language        = language
            )
        } catch(e: Exception) {
            exceptionB = e
        }
        assertNotNull(exceptionB, "Exception should be thrown on double posting")
    }

    @Test
    @Order(8)
    fun replies__will__trigger__email__to__all__subscribed__users__of__a__discussion() {
        clearDatabaseTables()
        val userX = User.createNormalUser("testX1@dev.bloip.com", "00000000")
        val userY = User.createNormalUser("testX2@dev.bloip.com", "00000000")

        val discussion = Discussion.create(
            userId          = userX.id,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         =  defaultCountry,
            eventSequenceId = "00000000",
            language        = language
        )
        Thread.sleep(1000)
        Discussion.reply(
            userId          = userY.id,
            discussionId    = discussion.id,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = "00000000",
            language        = language
        )
        Thread.sleep(1000)
        /** Verify email was sent **/
        assertEquals(1, TestUtils.numEmailsPresent(s3 = s3, emailBucket = applicationProperties.emailBucket))
    }

    @Test
    @Order(9)
    fun replies__will__not__trigger__email__to__users__with__email__disabled__() {
        clearDatabaseTables()
        var userX = User.createNormalUser("testXX2@dev.bloip.com", "00000000")
        val userY = User.createNewUser()

        val discussion = Discussion.create(
            userId          = userX.id,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         =  defaultCountry,
            eventSequenceId = "00000000",
            language        = language
        )
        Thread.sleep(1000)
        userX = User.findById(userId = userX.id)!!
        User.updateNotificationStatus(userId = userX.id, disabled = true, model = ExtendedModelMap() )

        Discussion.reply(
            userId          = userY.id,
            discussionId    = discussion.id,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = "00000000",
            language        = language
        )
        Thread.sleep(1000)
        /** Verify email was not sent **/
        assertEquals(0, TestUtils.numEmailsPresent(s3 = s3, emailBucket = applicationProperties.emailBucket))
    }
}