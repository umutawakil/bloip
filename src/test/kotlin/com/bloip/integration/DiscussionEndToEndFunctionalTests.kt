package com.bloip.integration

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.User
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.Title
import com.bloip.domain.localization.Country
import com.bloip.integration.utils.TestUtils
import com.bloip.services.DiscussionService
import com.bloip.services.UserService
import com.bloip.services.localization.CountryService
import com.bloip.services.localization.translation.LanguageService
import com.bloip.services.localization.translation.TranslationService
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.CollectingAlertHandler
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by Usman Mutawakil on 12/12/22.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["server.port=8443"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class DiscussionEndToEndFunctionalTests (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val userService: UserService,
    @Autowired private val countryService: CountryService,
    @Autowired private val discussionService: DiscussionService,
    @Autowired private val translationService: TranslationService,
    @Autowired private val languageService: LanguageService
  )
{
    private lateinit var webClient: WebClient
    private val URL = "https://localhost:8443"
    lateinit var defaultCountry: Country

    private var TEST_USER_NAME     = "DiscussionEndToEndFunctionalTests@dev.bloip.com"
    private var TEST_USER_PASSWORD = "xxxxxxxxxxx"
    private lateinit var testUser: User

    @BeforeAll
    fun init() {
        initClient()
        testUser                                        = userService.createAShogun(
            username = TEST_USER_NAME,
            password = TEST_USER_PASSWORD
        )
    }

    fun initClient() {
        webClient                                       = WebClient(BrowserVersion.CHROME)
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.cookieManager.isCookiesEnabled        = true
        webClient.options.isRedirectEnabled             = true
        webClient.options.isCssEnabled                  = false
        webClient.options.isJavaScriptEnabled           = true
        webClient.options.isUseInsecureSSL              = true
        defaultCountry                                  = countryService.getCanonicalByCode("us")!!
        //webClient.addRequestHeader("Origin", URL)
    }

    @AfterAll
    fun teardown() {
        webClient.close()
        discussionService.deleteAll()
        userService.delete(userId = testUser.id)
    }

    private fun createSimpleDiscussion(title: String, userId: Long) : Discussion {
        return discussionService.create(
            userId      = userId,
            title       = Title(title),
            ipAddress   = "localhost",
            duration    = 5,
            fileName    = "test.mp3",
            youtubeLink = null,
            country     = defaultCountry
        )
    }

    /** Test the UI blocks the user but also the service class itself does not allow the rule to be violated **/
    @Test
    @Order(0)
    fun will__block__user__from__exceeding__discussion__creation__limit() {
        applicationProperties.maxDiscussionCreationsPerDay = 1
        val discussion = createSimpleDiscussion(title = "TEST1", userId = testUser.id)

        var page: HtmlPage = loginToUseRootUserOnFrontEnd()
        val alertHandler = CollectingAlertHandler()
        webClient.alertHandler = alertHandler

        val newDiscussionLink: HtmlAnchor = TestUtils.getElementById(page,"new-discussion-link") as HtmlAnchor
        assertTrue(userService.isDiscussionCreationLimitReached(testUser))
        page = newDiscussionLink.click()
        Thread.sleep(1000)
        assertEquals("$URL/", page.baseURL.toString())
        assertEquals(
            1,
            alertHandler.collectedAlerts.size
        )

        val dialogs = translationService.getTranslationMap(
            context = "recording-states-dialogs",
            language = languageService.getCanonicalByCode("en")!!
        )
        assertEquals(
            dialogs["creation-limit-day"],
            alertHandler.collectedAlerts[0]
        )
          /**The service itself should explode if the UI is bypassed or the creation logic is used in another context outside
         * this UI example on the homepage**/
        var testEx: Exception? = null
        try {
            createSimpleDiscussion(title = "First double post test", userId = testUser.id)
        } catch (exception: Exception) {
            testEx = exception
        }
        assertNotNull(testEx)

        /** Test user can't reply to their own discussion *******************************/
        applicationProperties.maxDiscussionCreationsPerDay = 2
        page = loginToUseRootUserOnFrontEnd()
        page = webClient.getPage(URL + discussion.getEnglishUrl())
        var alertHandlerReply = CollectingAlertHandler()
        webClient.alertHandler = alertHandlerReply
        val replyButton: HtmlButton = TestUtils.getElementById(page,"reply-button") as HtmlButton
        page = replyButton.click()

        assertEquals("$URL/", page.baseURL.toString())
        assertEquals(
            1,
            alertHandlerReply.collectedAlerts.size
        )

        var dialogsReply = translationService.getTranslationMap(
            context = "recording-states-dialogs",
            language = languageService.getCanonicalByCode("en")!!
        )
        assertEquals(
            dialogsReply["reply-limit"],
            alertHandlerReply.collectedAlerts[0]
        )
    }

    @Test
    @Order(1)
    fun will__block__user__from__double__reply() {
        initClient()

        applicationProperties.maxDiscussionCreationsPerDay = 20
        var user: User   = userService.createNewUser()
        var discussion   = createSimpleDiscussion(title = "Second double post test", userId = user.id)

        discussionService.reply(
            userId       = testUser.id,
            discussionId = discussion.id,
            ipAddress    = "localhost",
            duration     = 5,
            fileName     = "test.webm"
        )

        var page               = loginToUseRootUserOnFrontEnd()
        page                   = webClient.getPage(URL + discussion.getEnglishUrl())
        val alertHandler       = CollectingAlertHandler()
        webClient.alertHandler = alertHandler

        val replyButton: HtmlButton = TestUtils.getElementById(page,"reply-button") as HtmlButton
        page = replyButton.click()
        assertEquals("$URL/", page.baseURL.toString())
        assertEquals(
            1,
            alertHandler.collectedAlerts.size
        )

        val dialogs = translationService.getTranslationMap(
                context  = "recording-states-dialogs",
                language = languageService.getCanonicalByCode("en")!!
        )
        assertEquals(
            dialogs["reply-limit"],
            alertHandler.collectedAlerts[0]
        )
        userService.delete(userId = user.id)
    }

    fun loginToUseRootUserOnFrontEnd() : HtmlPage {
        var logoutPage: HtmlPage          = webClient.getPage("$URL/bloip-logout")
        var page: HtmlPage                = webClient.getPage("$URL/bloip-settings")
        assertEquals("Login", page.titleText)

        val email: HtmlEmailInput         = TestUtils.getElementById(page,"email") as HtmlEmailInput
        val password: HtmlPasswordInput   = TestUtils.getElementById(page,"password") as HtmlPasswordInput
        val submitButton: HtmlSubmitInput = TestUtils.getElementById(page,"submit-button") as HtmlSubmitInput

        email.valueAttribute              = TEST_USER_NAME
        password.valueAttribute           = TEST_USER_PASSWORD
        page                              = submitButton.click()
        return TestUtils.getElementById(page, id = "logo-link")!!.click()
    }
}