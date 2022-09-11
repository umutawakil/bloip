package com.bloip.integration

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.domain.User
import com.bloip.domain.inbox.InboxItem
import com.bloip.repositories.*
import com.bloip.services.DiscussionService
import com.bloip.services.InboxService
import com.bloip.services.UserService
import com.bloip.structures.BumpStack
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by Usman Mutawakil on 9/8/22.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ReplyPathsIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val discussionSubscriptionRepository: DiscussionSubscriptionRepository,
    @Autowired val commentRepository: CommentRepository,
    @Autowired val userService: UserService,
    @Autowired val userRepository: UserRepository,
    @Autowired val userCookieRepository: UserCookieRepository,
    @Autowired val inboxService: InboxService,
    @Autowired val inboxRepository: InboxRepository
)
{
    private lateinit var userA: User
    private lateinit var userB: User
    private var numDiscussions               = 11
    var discussions: MutableList<Discussion> = mutableListOf()

    @BeforeAll
    fun setup() {
        println("BEFORE ALL...Going to clear various tables. Ensure DB is empty for associated tables")
        clearDatabaseTables()

        userA = userService.createNewUser()
        userB = userService.createNewUser()
        println("UserA: ${userA.id}, UserB: ${userB.id}")

        /** User A. Create a list of discussions for the sequence of tests. **/
        for(i in 0 until numDiscussions) {
            discussions.add(
                discussionService.create(
                    userId = userA.id,
                    title = "Why are raw oysters so expensive? ${i}",
                    ipAddress = "127.0.0.1")
            )
        }
        println("BEFORE ALL COMPLETE")
    }

    @AfterAll
    fun cleanup() {
        println("Cleaning up used tables")
        //TODO: There must be a better way of doing this.
       // clearDatabaseTables()
        println("AFTER ALL COMPLETE")
    }

    @Test
    @Order(0)
    fun verify_replies__exist__in__database() {
        applicationProperties.inboxItemsPerPage = 2
        /** Populate the inbox of UserA **/
        /** User B. trigger a reply notification for each discussion **/
        for (i in 0 until discussions.size) {
            discussionService.reply(userId = userB.id, discussionId = discussions[i].id, ipAddress = "127.0.0.1")
        }
        assertEquals(numDiscussions * 2, commentRepository.findAll().count())
    }

    @Test
    @Order(1)
    fun verify_inbox__exists__in__database() {
        assertEquals(numDiscussions, inboxRepository.findAll().count())
    }

    @Test
    @Order(2)
    fun can__paginate__through__inbox() {
        /** Verify User A's inbox total is updated correctly **/
        assertEquals(numDiscussions, inboxService.getInboxTotal(userId = userA.id))

        /** Walk the graph to verify content is paginated correctly in the bump stack **/
        paginateInbox(userId = userA.id, totalItems = numDiscussions, itemsPerPage = applicationProperties.inboxItemsPerPage)
    }

    @Test
    @Order(3)
    fun can__respond__to__inbox__messages() {
        /** Bring the inbox size per page to be the full set so we can retrieve all inbox items using a function actually called in prod.
         * The alternative would be to use a getAll type function but thats not used in this business context and would skip code branches
         * **/
        applicationProperties.inboxItemsPerPage = 11

        /** Respond to each message **/
        val userAInboxPage: BumpStack.Page<Long, InboxItem>  = inboxService.getNextPage(
            userId = userA.id, offsetKey = null)

        userAInboxPage.values.forEach { x ->
            discussionService.reply(
                userId       = userA.id,
                discussionId = x.discussionId,
                ipAddress    = "127.0.0.1"
            )
        }
        val inboxItemA = userAInboxPage.values[0]

        /** Verify User B's inbox total is updated correctly **/
        assertEquals(numDiscussions, inboxService.getInboxTotal(userId = userB.id))
        val inboxItemB: InboxItem = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]

        assertEquals(1, inboxItemB.trackNumber)
        assertEquals(inboxItemA.discussionId, inboxItemB.discussionId)
        assertEquals(inboxItemA.count,inboxItemB.count)
        assertNotEquals(0,inboxItemB.count)
    }

    /** User can unsubscribe, be skipped a notification, resubscribe, then receive new notifications **/
    @Test
    @Order(4)
    fun can__toggle__inbox__subscriptions() {
        var inboxitem1: InboxItem = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]

        val numSubscriptions: Int = discussionSubscriptionRepository.findAll().count()
        discussionService.unsubscribe(
            userId       = userA.id,
            discussionId = inboxitem1.discussionId
        )
        assertEquals(numSubscriptions - 1, discussionSubscriptionRepository.findAll().count()) //verify db changes

        //verify the total and verify the individual item directly as the two values hold there own state and are not just equations
        discussionService.reply(userId = userB.id, discussionId = inboxitem1.discussionId, ipAddress = "127.0.0.1")
        assertEquals(numDiscussions, inboxService.getInboxTotal(userId = userA.id))
        val inboxitem2 = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]
        assertEquals(inboxitem1.discussionId, inboxitem2.discussionId)
        assertEquals(inboxitem1.count,inboxitem2.count)

        //Now resubscribe A, have b send out a notification and confirm A's inbox is modified accordingly
        discussionService.subscribe(
            userId       = userA.id,
            discussionId = inboxitem2.discussionId
        )
        assertEquals(numSubscriptions, discussionSubscriptionRepository.findAll().count()) //Verify DB changes

        discussionService.reply(userId = userB.id, discussionId = inboxitem2.discussionId, ipAddress = "127.0.0.1")
        assertEquals(numDiscussions + 1, inboxService.getInboxTotal(userId = userA.id))
        val inboxitem3: InboxItem = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]
        assertEquals(inboxitem2.discussionId, inboxitem3.discussionId)
        assertEquals(inboxitem2.count,inboxitem3.count)
        assertNotEquals(0, inboxitem3.count)
    }

    /** User A can delete an item from the inbox and still receive new notifications in that conversation **/
    @Test
    @Order(5)
    fun can__delete__inbox__items__without__unsubscribing() {
        val inboxitem3: InboxItem = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]

        val previousTotal = inboxService.getInboxTotal(userId = userA.id)
        inboxService.deleteConversation(userId = userA.id, discussionId = inboxitem3.discussionId)
        val pageOfDelete: BumpStack.Page<Long, InboxItem> = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        )
        val inboxitemNewHead: InboxItem = pageOfDelete.values[0]
        val currentTotal = inboxService.getInboxTotal(userId = userA.id)
        assertNotEquals(inboxitem3.discussionId, inboxitemNewHead.discussionId)
        assertEquals(inboxitem3.count, previousTotal - currentTotal) //deletion took 2 values a way since that item was 2 higher

        discussionService.reply(userId = userB.id, discussionId = inboxitem3.discussionId, ipAddress = "127.0.0.1")
        val inboxitem4: InboxItem = inboxService.getNextPage(
            userId    = userA.id,
            offsetKey = null
        ).values[0]
        assertEquals(currentTotal + 1, inboxService.getInboxTotal(userId = userA.id))
        assertEquals(inboxitem3.discussionId, inboxitem4.discussionId)
        assertEquals(1, inboxitem4.count)
    }

    //@Test
    @Order(6)
    fun can__trigger__multiple__replies__notifications() {
        val numUsers: Int = 10

        val firstUser: User = userService.createNewUser()
        val users: MutableList<User> = mutableListOf()
        for(i in 0 until numUsers) {
            users.add(userService.createNewUser())
        }

        val discussion = discussionService.create(
            userId = firstUser.id,
            title = "Why are raw oysters so expensive?",
            ipAddress = "127.0.0.1"
        )

        //Users comment
        for(user in users) {
            discussionService.reply(
                userId = user.id,
                discussionId = discussion.id,
                ipAddress = "127.0.0.1"
            )
        }

        //First user responds
        assertEquals(numUsers, inboxService.getInboxTotal(userId = firstUser.id))
        discussionService.reply(
            userId = firstUser.id,
            discussionId = discussion.id,
            ipAddress = "127.0.0.1"
        )

        //Verify recipients inboxes and that they can respond
        for(user in users) {
            assertEquals(1, inboxService.getInboxTotal(userId = user.id))

            val inboxitem: InboxItem = inboxService.getNextPage(
                userId    = user.id,
                offsetKey = null
            ).values[0]

            discussionService.reply(
                userId = user.id,
                discussionId = discussion.id,
                ipAddress = "127.0.0.1"
            )
        }

        //Verify the initial senders inbox is correct
        assertEquals(numUsers * 2, inboxService.getInboxTotal(userId = firstUser.id))
        val inboxitem: InboxItem = inboxService.getNextPage(
            userId    = firstUser.id,
            offsetKey = null
        ).values[0]
        assertEquals(numUsers * 2, inboxitem.count)
    }

    fun paginateInbox(userId: Long, totalItems: Int, itemsPerPage: Int) {
        /** From left to right **/
        var p = 0
        var numOfPages: Int = (totalItems / itemsPerPage) + (totalItems % itemsPerPage)
        var offsetKey: Long? = null
        var tempPage: BumpStack.Page<Long, InboxItem>? = null

        while(p < numOfPages) {
            tempPage = inboxService.getNextPage(userId = userId, offsetKey = offsetKey)

            println("P: ${tempPage.values.size}")

            if(p < numOfPages - 1) {
                Assertions.assertNotNull(tempPage.nextOffsetKey)
            }
            if (p > 0) {
                Assertions.assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (p == numOfPages - 1) {
                Assertions.assertTrue(tempPage.values[0].title.contains("0"))
            }

            offsetKey = tempPage.nextOffsetKey
            p++
        }

        /** From right to left **/
        /** Note - The previous function is exclusive to the starting offset where as next is inclusive **/
        var x = 0
        while(tempPage!!.previousOffsetKey != null) {
            tempPage = inboxService.getPreviousPage(userId = userId, offsetKey = tempPage.previousOffsetKey!!)
            if (x > 0) {
                Assertions.assertNotNull(tempPage.nextOffsetKey)
            }
            if (x < numOfPages - 2) {
                Assertions.assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (x == numOfPages - 2) {
                Assertions.assertTrue(tempPage.values[0].title.contains("${totalItems - 1}"))
            }
            x++
        }
        assertEquals(numOfPages - 1, x)
    }

    fun clearDatabaseTables() {
        userRepository.findAll().forEach { x -> userRepository.delete(x) }
        userCookieRepository.findAll().forEach { x -> userCookieRepository.delete(x) }
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }
        commentRepository.findAll().forEach { x -> commentRepository.delete(x) }
        inboxRepository.findAll().forEach { x -> inboxRepository.delete(x) }
        discussionSubscriptionRepository.findAll().forEach { x -> discussionSubscriptionRepository.delete(x) }
    }
}