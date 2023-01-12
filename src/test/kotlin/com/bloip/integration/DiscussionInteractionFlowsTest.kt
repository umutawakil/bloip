package com.bloip.integration

import com.bloip.caches.DiscussionCache
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.localization.Country
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.user.User
import com.bloip.domain.discussion.value.Title
import com.bloip.integration.mocks.MockMediaConversionService
import com.bloip.repositories.DiscussionRepository
import com.bloip.services.localization.CountryService
import com.bloip.services.DiscussionService
import com.bloip.structures.BumpStack
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
class DiscussionInteractionFlowsTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val discussionService: DiscussionService,
    @Autowired val discussionRepository: DiscussionRepository,
    @Autowired val discussionCache: DiscussionCache,
    @Autowired val countryService: CountryService
) {

    var expectedTitle: Title = Title("Why is it hard to raise clams indoors?") //This is for the one standout discussion to be created last added on top of the stack
    val numDiscussions: Int = 10
    var discussionsPerPage  = 2
    lateinit var discussion: Discussion

    lateinit var page: BumpStack.Page<Long, Discussion>
    lateinit var databaseResults: List<Discussion>

    lateinit var defaultCountry: Country
    var eventSequenceId = "55555555555555555"

    var previousDiscussionsPerPage = 0

    @BeforeAll
    fun setup() {
        deleteCertainTables()

        applicationProperties.enableRemoteServices="NO"
        discussionService.mediaConversionService = MockMediaConversionService()
        defaultCountry = countryService.getCanonicalByCode("us")!!

        previousDiscussionsPerPage = applicationProperties.discussionsPerPage
    }

    @AfterAll
    fun cleanup() {
       deleteCertainTables()
       applicationProperties.discussionsPerPage = previousDiscussionsPerPage
    }

    private fun deleteCertainTables() {
        User.deleteAll()
        discussionRepository.findAll().forEach { x -> discussionRepository.delete(x) }
    }

    @Test
    @Order(0)
    fun can_create_a__new__discussion() {
        applicationProperties.discussionsPerPage = discussionsPerPage

        for(i in 0 until (numDiscussions - 1)) {
            //Thread.sleep(100)
            assertNotNull(
                discussionService.create(user = User.createNewUser(),
                    title           = Title("Why are raw oysters so expensive? $i"),
                    duration        = 30,
                    fileName        = "test.webm",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId
                )
            )
        }
        /** Verify the media conversion service is running on NON-mp4 files **/
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).count == numDiscussions - 1)


        /** Run again on different file type **/
        discussionService.mediaConversionService = MockMediaConversionService()
        discussion = discussionService.create(
            user      = User.createNewUser(),
            title     = expectedTitle,
            duration  = 30,
            fileName  = "test.mp4",
            country   = defaultCountry,
            eventSequenceId = eventSequenceId
        )
        assertEquals(expectedTitle, discussion.title)

        /** Verify the media conversion service is not running on mp4 files **/
        assertFalse((discussionService.mediaConversionService as MockMediaConversionService).ran)
        assertTrue((discussionService.mediaConversionService as MockMediaConversionService).count == 0)
    }

    /** Verify the cache and database are in sync.
     * THIS TEST HAS PROVEN ITSELF BEFORE DESPITE HOW ARBITRARY IT MAY SEEM. DON'T REMOVE FOR AESTHETIC REASONS
     * **/
    @Test
    @Order(1)
    fun verify__cache__and__DB__are__in__sync() {
        println("New discussion ID: ${discussion.id}")
        assertEquals(discussion, discussionCache.get(discussionId = discussion.id))
        assertEquals(discussion, discussionRepository.findById(discussion.id).get())
        page = discussionService.getNextPage(country = defaultCountry, null)
        databaseResults = discussionRepository.findAllAscending()
        assertEquals(numDiscussions, databaseResults.size)
    }

    /** Verify cache metadata matches the database and that both are at the top of the cache and db.
     * Remember bump stack reverses order of input
     */
    @Test
    @Order(2)
    fun verify__cache__metadata__matches__DB__and__order__matches() {
        assertEquals(discussion, page.values[0])
        assertEquals(discussionsPerPage, page.values.size)
        assertNotNull(page.nextOffsetKey)
        assertNull(page.previousOffsetKey)

        val field: Field   = Discussion::class.java.superclass.getDeclaredField("id")
        field.isAccessible = true
        assertEquals(field.get(discussion) as Long, field.get(databaseResults[databaseResults.size - 1]) as Long) // DB retrieved in ASC order
    }

    @Test
    @Order(3)
    fun can__paginate__properly() {
        /** Verify the results are paginated properly **/

        /** From left to right **/
        var p = 0
        val numOfPages: Int = (numDiscussions / discussionsPerPage) + (numDiscussions % discussionsPerPage)
        var offsetKey: Long? = null
        var tempPage: BumpStack.Page<Long, Discussion>? = null

        /** From left to right **/
        while(p < numOfPages) {
            /** Verify the pagination range boundaries are the expected pair signifying you are going forward **/
            tempPage = discussionService.getNextPage(country = defaultCountry, offsetKey = offsetKey)
            if(p < numOfPages - 1) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (p > 0) {
                assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (p == numOfPages - 1) {
                assertTrue(tempPage.values[tempPage.values.size - 1].title.value.contains("0"))
            }

            offsetKey = tempPage.nextOffsetKey
            p++
        }

        /** From right to left **/
        /** Note - The previous function is exclusive to the starting offset whereas next is inclusive **/
        var x = 0
        while(tempPage!!.previousOffsetKey != null) {
            /** Verify the pagination range boundaries are the expected pair signifying you are going backward **/
            tempPage = discussionService.getPreviousPage(country = defaultCountry, offsetKey = tempPage.previousOffsetKey!!)
            if (x > 0) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (x < numOfPages - 2) {
               assertNotNull(tempPage.previousOffsetKey)
            }

            /** Verify the last element added to the bump stack is first element on the first page.**/
            if (x == numOfPages - 2) {
                assertTrue(tempPage.values[0].title.value.contains("clams")) //Clams is the 'magic' word from the last title that is now in first position.
            }
            x++
        }
        assertEquals(numOfPages - 1, x)
    }


    //TODO: Some voodoo magic happens here if theres no Thread.sleep and ALL tests are run. From what I gather on SO the applicationProperties file could be replaced
    //TODO: by the compiler so at run time the value at maxDiscussions is different from the value that is set in the chain of beans using the value
    //TODO: Outside of testing this is probably a good reason to not ever use spring beans as mutable data stores for logic to depend on. Perhaps only if the context is bound
    //TODO: Could be that if theres no explicit dependency each of these tests tries to run concurrently despite the sequence order and the altered value arrives late inside the calling code below after the loop has started.
    @Test
    @Order(4)
    fun can__not__create__unlimited__discussions__in__one__day() {
        synchronized(
            applicationProperties.maxDiscussionCreationsPerDay
        ) {
            //Thread.sleep(2000)
            val userX = User.createNewUser()
            var exception: Exception? = null
            try {
                for (i in 0 until applicationProperties.maxDiscussionCreationsPerDay + 1) {
                    /** The reason this println statement makes this work might be that the transition from completing this create and reading/updating
                     * the users discussion count may not be refelected as quickly as I would think. Not sure why when every save is done synchronously.
                     * Might have something to do with how the compiler unrolls the loop.
                     * **/
                    println("TestMax i-> $i ->: ${applicationProperties.maxDiscussionCreationsPerDay}")
                    //Thread.sleep(100)
                    discussionService.create(
                        user            = User.findById(userId = userX.id)!!,
                        title           = Title("Creating unlimited discussions in a day!!!! $i"),
                        duration        = 30,
                        fileName        = "test.webm",
                        country         = defaultCountry,
                        eventSequenceId = eventSequenceId
                    )
                }
            } catch (e: Exception) {
                exception = e
            }
            assertNotNull(exception, "Should return exception when limit of discussion creations is exceeded")
        }
    }
}