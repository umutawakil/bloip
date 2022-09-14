package com.bloip.integration

import com.bloip.caches.DiscussionCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.domain.Topic
import com.bloip.domain.User
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.DiscussionService
import com.bloip.services.TopicService
import com.bloip.services.UserService
import com.bloip.structures.BumpStack
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by Usman Mutawakil on 9/8/22.
 */

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DiscussionPathsIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val discussionCache: DiscussionCache,
    @Autowired val userService: UserService,
    @Autowired val topicService: TopicService,
) {

    lateinit var user: User
    var expectedTitle = "Why is it hard to raise clams indoors?" //This is for the one standout discussion to be created last added ontop of the stack
    var numDiscussions = 11
    var discussionsPerPage = 2
    lateinit var discussion: Discussion

    lateinit var page: BumpStack.Page<Long, Discussion>
    lateinit var databaseResults: List<Discussion>
    lateinit var topic: Topic

    @BeforeAll
    fun setup() {
        /** Cleanup **/
        deleteCertainTables()

        topic = topicService.get(friendlyId = "cops")!!
    }
    @AfterAll
    fun cleanup() {
        /** Cleanup **/
       deleteCertainTables()
    }

    fun deleteCertainTables() {
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }
    }

    @Test
    @Order(0)
    fun can_create_a__new__discussion() {
        user = userService.createNewUser()
        applicationProperties.discussionsPerPage = discussionsPerPage

        for(i in 0 until (numDiscussions - 1)) {
            discussionService.create(userId = user.id,
                title = "Why are raw oysters so expensive? ${i}",
                topic = topic,
                ipAddress = "127.0.0.1"
            )
        }

        discussion = discussionService.create(userId = user.id, title = expectedTitle, topic = topic, ipAddress = "127.0.0.1")
        assertEquals(expectedTitle, discussion.title)
    }

    /** Verify the cache and database are in sync. **/
    @Test
    @Order(1)
    fun verify__cache__and__DB__are__in__sync() {
        assertEquals(discussion, discussionCache.get(discussionId = discussion.id))
        assertEquals(discussion, discussionRepository.findById(discussion.id).get())
        page = discussionService.getNextPage(null)
        databaseResults = discussionRepository.findAllAscending()
        assertEquals(numDiscussions, databaseResults.size)
    }

    /** Verify cache metadata matches the database and that both are at the top of the cache and db.
     * Remember bump stack reverses order of input
     */
    @Test
    @Order(2)
    fun verify__cache__metadata__matches__DB__and__order__matches() {
        assertNotNull(page.nextOffsetKey)
        assertNull(page.previousOffsetKey)
        assertEquals(discussionsPerPage, page.values.size)
        assertEquals(discussion, page.values[0])
        assertEquals(discussion, databaseResults[databaseResults.size - 1]) // DB retrieved in ASC order
    }
    @Test
    fun can__paginate__properly() {
        /** Verify the results are paginated properly **/

        /** From left to right **/
        var p = 0
        var numOfPages: Int = (numDiscussions / discussionsPerPage) + (numDiscussions % discussionsPerPage)
        var offsetKey: Long? = null
        var tempPage: BumpStack.Page<Long, Discussion>? = null

        while(p < numOfPages) {
            tempPage = discussionService.getNextPage(offsetKey = offsetKey)
            if(p < numOfPages - 1) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (p > 0) {
                assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (p == numOfPages - 1) {
                assertTrue(tempPage.values[0].title.contains("0"))
            }

            offsetKey = tempPage.nextOffsetKey
            p++
        }

        /** From right to left **/
        /** Note - The previous function is exclusive to the starting offset where as next is inclusive **/
        var x = 0
        while(tempPage!!.previousOffsetKey != null) {
            tempPage = discussionService.getPreviousPage(offsetKey = tempPage.previousOffsetKey!!)
            if (x > 0) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (x < numOfPages - 2) {
               assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (x == numOfPages - 2) {
                assertTrue(tempPage.values[0].title.contains("clams"))
            }
            x++
        }
        assertEquals(numOfPages - 1, x)
    }
}