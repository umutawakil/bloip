package com.bloip.integration

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.UserEvent
import com.bloip.domain.localization.Country
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.user.User
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Language
import com.bloip.repositories.GenericRepository
import com.bloip.services.localization.CountryService
import com.bloip.services.localization.translation.LanguageService

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
class DiscussionBackEndTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val genericRepository: GenericRepository,
    @Autowired val countryService: CountryService,
    @Autowired val languageService: LanguageService
) {

    var expectedTitle: Title = Title("Why is it hard to raise clams indoors?") //This is for the one standout discussion to be created last added on top of the stack
    var numDiscussions: Int = 10
    var discussionsPerPage  = 2
    var discussion: Discussion? = null

    lateinit var page: BumpStack.Page<Discussion.DiscussionId, Any>
    lateinit var databaseResults: List<Discussion>

    private lateinit var defaultCountry: Country
    private lateinit var language: Language
    var eventSequenceId = "55555555555555555"

    var previousDiscussionsPerPage = 0

    @BeforeAll
    fun setup() {
        deleteCertainTables()

        applicationProperties.enableRemoteServices = "NO"
        defaultCountry                             = countryService.getCanonicalByCode("us")!!
        previousDiscussionsPerPage                 = applicationProperties.discussionsPerPage
        language                                   = languageService.getCanonicalByCode(code = "en")!!
    }

    @AfterAll
    fun cleanup() {
       deleteCertainTables()
       applicationProperties.discussionsPerPage = previousDiscussionsPerPage
    }

    private fun deleteCertainTables() {
        UserEvent.deleteAll()
        User.deleteAll()
        Discussion.deleteAll()
    }

    @Test
    @Order(0)
    fun can_create_a__new__discussion() {
        applicationProperties.discussionsPerPage = discussionsPerPage

        for(i in 0 until numDiscussions) {
            val currentUserId = User.createNewUser().id
            val d = Discussion.create(
                userId          = currentUserId,
                title           = Title("Why are raw oysters so expensive? $i"),
                duration        = 30,
                fileName        = "test.webm",
                country         = defaultCountry,
                eventSequenceId = eventSequenceId,
                language        = language
            )
            d.testVerifyTrackNumber(potentialTrackNumber = 1)
            assertNotNull(d)
        }
        /** Verify the media conversion service is running on NON-mp4 files **/
        assertEquals(numDiscussions, UserEvent.numOfConversionRequests())
        UserEvent.deleteAll()

        /** Run again on different file type **/
        val randomUser = User.createNewUser()
        discussion = Discussion.create(
            userId          = randomUser.id,
            title           = expectedTitle,
            duration        = 30,
            fileName        = "test.mp4",
            country         = defaultCountry,
            eventSequenceId = eventSequenceId,
            language        = language
        )
        numDiscussions++
        /** Verify the media conversion service is NOT running on mp4 files **/
        //assertTrue(UserEvent.findByUserIdAndName(userId = randomUser.id, name = "conversion_request").isEmpty())
        assertEquals(0, UserEvent.numOfConversionRequests())
        UserEvent.deleteAll()
    }

    /** Verify the cache and database are in sync.
     * THIS TEST HAS PROVEN ITSELF BEFORE DESPITE HOW ARBITRARY IT MAY SEEM. DON'T REMOVE FOR AESTHETIC REASONS
     * **/
    @Test
    @Order(1)
    fun verify__cache__and__DB__are__in__sync() {
        assertEquals(discussion, Discussion.get(discussionId = discussion!!.id))

        //TODO: Needs a getFromDatabase mono lookup or verification just for testing. Then the DiscussionId properties can all be made private
        assertEquals(discussion, genericRepository.findById(id = discussion!!.id.discussionId, targetClass = Discussion::class.java))
        page = Discussion.getNextPage(country = defaultCountry, null)
        databaseResults = genericRepository.findAllBy(query="SELECT d FROM Discussion d ORDER BY d.id DESC", targetClass = Discussion::class.java)
        assertEquals(numDiscussions, databaseResults.size)
    }

    /** Verify cache metadata matches the database and that both are at the top of the cache and db.
     * Remember bump stack reverses order of input
     */
    @Test
    @Order(2)
    fun verify__cache__metadata__matches__DB__and__order__matches() {
        Discussion.testVerifyDiscussionDtoId(dto = page.values[0], discussion!!.id)
        //assertEquals(discussion!!.id, page.values[0].id)
        assertEquals(discussionsPerPage, page.values.size)
        assertNotNull(page.nextOffsetKey)
        assertNull(page.previousOffsetKey)
        assertEquals(discussion, databaseResults[0])
    }

    @Test
    @Order(3)
    fun can__paginate__properly() {
        /** Verify the results are paginated properly **/

        /** From left to right **/
        var p = 0
        val numOfPages: Int = (numDiscussions / discussionsPerPage) + (numDiscussions % discussionsPerPage)
        var offsetKey: Discussion.DiscussionId? = null
        var tempPage: BumpStack.Page<Discussion.DiscussionId, Any>? = null

        /** From left to right **/
        while(p < numOfPages) {
            /** Verify the pagination range boundaries are the expected pair signifying you are going forward **/
            tempPage = Discussion.getNextPage(country = defaultCountry, offsetKey = offsetKey)
            if(p < numOfPages - 1) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (p > 0) {
                assertNotNull(tempPage.previousOffsetKey)
            }

            /** Just verify the first element added is last. **/
            if (p == numOfPages - 1) {
                assertTrue(Discussion.testVerifyTitleContains(dto = tempPage.values[tempPage.values.size - 1], "0"))
            }

            offsetKey = tempPage.nextOffsetKey
            p++
        }

        /** From right to left **/
        /** Note - The previous function is exclusive to the starting offset whereas next is inclusive **/
        var x = 0
        while(tempPage!!.previousOffsetKey != null) {
            /** Verify the pagination range boundaries are the expected pair signifying you are going backward **/
            tempPage = Discussion.getPreviousPage(country = defaultCountry, offsetKey = tempPage.previousOffsetKey!!)
            if (x > 0) {
                assertNotNull(tempPage.nextOffsetKey)
            }
            if (x < numOfPages - 2) {
               assertNotNull(tempPage.previousOffsetKey)
            }

            /** Verify the last element added to the bump stack is first element on the first page.**/
            if (x == numOfPages - 2) {
                assertTrue(Discussion.testVerifyTitleContains(dto = tempPage.values[0],"clams")) //Clams is the 'magic' word from the last title that is now in first position.
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
        val userX: User = User.createNewUser()
        var exception: Exception? = null
        try {
            for (i in 0 until User.maxDiscussionCreationsPerDay + 1) {
                Discussion.create(
                    userId          = userX.id,
                    title           = Title("Creating unlimited discussions in a day!!!! $i"),
                    duration        = 30,
                    fileName        = "test.webm",
                    country         = defaultCountry,
                    eventSequenceId = eventSequenceId,
                    language        = language
                )
            }
        } catch (e: Exception) {
            exception = e
        }
        assertNotNull(exception, "Should return exception when limit of discussion creations is exceeded")
    }
}