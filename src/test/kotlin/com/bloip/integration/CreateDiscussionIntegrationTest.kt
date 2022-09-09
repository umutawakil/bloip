package com.bloip.integration

import com.bloip.caches.DiscussionCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Discussion
import com.bloip.domain.User
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.DiscussionService
import com.bloip.services.UserService
import com.bloip.structures.BumpStack
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.transaction.annotation.Transactional

/**
 * Created by Usman Mutawakil on 9/8/22.
 */

@SpringBootTest
@Rollback
@Transactional
class CreateDiscussionIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val discussionCache: DiscussionCache,
    @Autowired val userService: UserService
) {
    @Test
    fun can__create__discussion() {
        /** Cleanup **/
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }

        val user: User = userService.createNewUser()
        val expectedTitle = "Why is it hard to raise clams indoors?"
        val numDiscussions = 11
        val discussionsPerPage = 2
        applicationProperties.discussionsPerPage = discussionsPerPage

        for(i in 0 until (numDiscussions - 1)) {
            var output = "Why are raw oysters so expensive? ${i}"
            discussionService.create(userId = user.id, title = output, ipAddress = "127.0.0.1")
        }

        val discussion: Discussion = discussionService.create(userId = user.id, title = expectedTitle, ipAddress = "127.0.0.1")
        assertEquals(expectedTitle, discussion.title)

        /** Verify the cache and database are in sync. **/
        assertEquals(discussion, discussionCache.get(discussionId = discussion.id))
        assertEquals(discussion, discussionRepository.findById(discussion.id).get())

        val page: BumpStack.Page<Long, Discussion> = discussionService.getNextPage(null)
        val databaseResults = discussionRepository.findAllAscending()

        /** Verify cache metadata matches the database and that both are at the top of the cache and db.
         * Remember bump stack reverses order of input
         */
        assertNotNull(page.nextOffsetKey)
        assertNull(page.previousOffsetKey)
        assertEquals(discussionsPerPage, page.values.size)
        assertEquals(discussion, page.values[0])
        assertEquals(discussion, databaseResults[databaseResults.size - 1]) // DB retrieved in ASC order

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