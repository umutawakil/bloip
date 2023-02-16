package com.bloip.integration

import com.bloip.domain.UserEvent
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Discussion.DiscussionId
import com.bloip.domain.user.User.UserId
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Country
import com.bloip.domain.localization.Language
import com.bloip.domain.user.User

import com.bloip.services.localization.CountryService
import com.bloip.services.localization.translation.LanguageService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadTest(
    @Autowired private val countryService: CountryService,
    @Autowired private val languageService: LanguageService
)
{
    private lateinit var defaultCountry: Country
    private lateinit var language: Language
    val numThreads          = 15
    val numRepliesPerThread = 10

    var start               = 0L
    @BeforeAll
    fun init() {
        cleanup()
        defaultCountry = countryService.getCanonicalByCode(code = "us")!!
        language       = languageService.getCanonicalByCode(code = "en")!!
    }

    @AfterAll
    fun shutdown() {
        cleanup()
    }

    private fun cleanup() {
        User.deleteAll()
        UserEvent.deleteAll()
        Discussion.deleteAll()
    }

    @Test
    fun can__handle__many__current__replies__in__a__discussion() {
        val user : User  = User.createNewUser()
        val discussionId = createSimpleDiscussion(userId = user.id).id

        val testThreads = mutableListOf<Thread>()
        start = System.currentTimeMillis() / 1000L
        for (i in 0 until numThreads) {
            testThreads.add(
            Thread {
                Thread.currentThread().name = "Worker$i"
                performSequenceOfRepliesWithRandomUsers(
                    discussionId = discussionId,
                    numReplies   = numRepliesPerThread
                )
            })
            testThreads[i].start()
        }
        for(t in testThreads) {
            t.join()
        }
        println("Completion Time: " + ((System.currentTimeMillis() / 1000L) - start) + " seconds")


        /** Test Code below **/
        val discussion = Discussion.get(discussionId = discussionId)!!

        discussion.testVerifyReplyCount(
            potentialCount = numThreads * numRepliesPerThread
        )
        Discussion.testVerifyInboxTotal(
            total = calculateInboxSumAcrossAllUsers
                (
                    numThreads          = numThreads,
                    numRepliesPerThread = numRepliesPerThread
                )
        )
    }

    private fun calculateInboxSumAcrossAllUsers(numThreads: Int, numRepliesPerThread: Int) : Int {
        val totalPosts = (numThreads * numRepliesPerThread) + 1
        println("Total posts: $totalPosts")

        val total = (totalPosts * (totalPosts - 1)) / 2
        println("Total = $totalPosts * ($totalPosts - 1) / 2")
        println("Total notifications across all users: $total")

        return total
    }

    private fun performSequenceOfRepliesWithRandomUsers(discussionId: DiscussionId, numReplies: Int) {
        for (i in 0 until numReplies) {
            simpleReply(
                discussionId = discussionId,
                userId       = User.createNewUser().id
            )
        }
    }

    private fun createSimpleDiscussion(userId: UserId) : Discussion {
        return Discussion.create(
            userId          = userId,
            title           = Title("Why are raw oysters so expensive? ${System.currentTimeMillis()}"),
            duration        = 20,
            fileName        = "test.mp3",
            country         = defaultCountry,
            eventSequenceId = "123",
            language        = language
        )
    }

    private fun simpleReply(discussionId: DiscussionId, userId: UserId) {
        Discussion.reply(
            userId          = userId,
            discussionId    = discussionId,
            duration        = 30,
            fileName        = "test.mp3",
            eventSequenceId = "eventSequenceId",
            language        = language
        )
    }
}