package com.bloip.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.user.User
import com.bloip.domain.discussion.Discussion
import com.bloip.domain.discussion.value.Title
import com.bloip.domain.localization.Country
import com.bloip.domain.localization.Language
import com.bloip.integration.utils.TestUtils

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
class DiscussionEndToEndTests (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val countryService: CountryService,
    @Autowired private val translationService: TranslationService,
    @Autowired private val languageService: LanguageService
  )
{
    private lateinit var webClient: WebClient
    private val URL = "https://localhost:8443"
    private lateinit var defaultCountry: Country
    private lateinit var language: Language

    private var TEST_USER_NAME     = "DiscussionEndToEndFunctionalTests@dev.bloip.com"
    private var TEST_USER_PASSWORD = "xxxxxxxxxxx"
    private lateinit var testUser: User

    private lateinit var s3: AmazonS3

    private var originalDiscussionLimit = 0

    @BeforeAll
    fun init() {
        User.deleteAll()
        initClient()
        testUser = User.createAShogun(
            username = TEST_USER_NAME,
            password = TEST_USER_PASSWORD
        )
        s3 = AmazonS3ClientBuilder.standard().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        ).build()
        clearEmailBucket()

        language = languageService.getCanonicalByCode(code = "en")!!

        originalDiscussionLimit = User.maxDiscussionCreationsPerDay
    }

    fun clearEmailBucket() {
        val objectList = s3.listObjects(applicationProperties.emailBucket)
        for(s in objectList.objectSummaries) {
            s3.deleteObject(applicationProperties.emailBucket, s.key)
        }
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
        webClient.addRequestHeader("Origin", URL)
    }

    @AfterAll
    fun teardown() {
        webClient.close()
        Discussion.deleteAll()
        User.deleteAll()
        clearEmailBucket()

        User.maxDiscussionCreationsPerDay = originalDiscussionLimit
    }

    private fun createSimpleDiscussion(title: String, user: User) : Discussion {
        return Discussion.create(
            userId          = user.id,
            title           = Title(title),
            duration        = 5,
            fileName        = "test.mp3",
            country         = defaultCountry,
            language        = language
        )
    }

    /** Test the UI blocks the user but also the service class itself does not allow the rule to be violated **/
    @Test
    @Order(0)
    fun will__block__user__from__exceeding__discussion__creation__limit() {
        User.maxDiscussionCreationsPerDay = 1
        val discussion = createSimpleDiscussion(title = "TEST1", user = testUser)
        testUser = User.findById(userId = testUser.id)!!

        var page: HtmlPage = loginToUseRootUserOnFrontEnd()
        Thread.sleep(5000)
        val alertHandler = CollectingAlertHandler()
        webClient.alertHandler = alertHandler

        val newDiscussionLink: HtmlAnchor = TestUtils.getElementById(page,"new-discussion-link") as HtmlAnchor
        testUser = User.findById(userId = testUser.id)!!
        //assertTrue(testUser.isDiscussionCreationLimitReached())
        Thread.sleep(1000)
        page = newDiscussionLink.click()
        Thread.sleep(3000)
        testUser = User.findById(userId = testUser.id)!!

        assertEquals("$URL/", page.baseURL.toString())
        assertEquals(1,alertHandler.collectedAlerts.size)

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
            createSimpleDiscussion(title = "First double post test", user = testUser)
        } catch (exception: Exception) {
            testEx = exception
        }
        assertNotNull(testEx)

        /** Test user can't reply to their own discussion *******************************/
        User.maxDiscussionCreationsPerDay = 2
        page = loginToUseRootUserOnFrontEnd()
        Thread.sleep(5000)
        page = discussion.goToDiscussionPageWithBaseUrl(webClient = webClient, url = URL)
        Thread.sleep(1000)
        val alertHandlerReply = CollectingAlertHandler()
        webClient.alertHandler = alertHandlerReply
        val replyButton: HtmlButton = TestUtils.getElementById(page,"reply-button") as HtmlButton
        page = replyButton.click()
        Thread.sleep(1000)

        assertEquals("$URL/", page.baseURL.toString())
        assertEquals(
            1,
            alertHandlerReply.collectedAlerts.size
        )

        val dialogsReply = translationService.getTranslationMap(
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

        //applicationProperties.maxDiscussionCreationsPerDay = 20
        testUser = User.findById(userId = testUser.id)!!

        val discussion   = createSimpleDiscussion(title = "Second double post test", user = User.createNewUser())

        val updatedDiscussion = Discussion.reply(
            userId          = testUser.id,
            discussionId    = discussion.id,
            duration        = 5,
            fileName        = "test.webm",
            language        = language
        )

        var page               = loginToUseRootUserOnFrontEnd()
        Thread.sleep(5000)
        page                   = updatedDiscussion.goToDiscussionPageWithBaseUrl(webClient = webClient, url = URL)
        Thread.sleep(1000)
        val alertHandler       = CollectingAlertHandler()
        webClient.alertHandler = alertHandler

        val replyButton: HtmlButton = TestUtils.getElementById(page,"reply-button") as HtmlButton
        page = replyButton.click()
        Thread.sleep(1000)
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
    }

    @Test
    @Order(2)
    fun users__can__disable__reply__emails__() {
        var userX = User.createNormalUser("test4@dev.bloip.com", "XXXXXXXX")
        val userY = User.createNewUser()

        val discussion = Discussion.create(
            userId          = userX.id,
            title           = Title("Why are raw oysters so expensive?"),
            duration        = 30,
            fileName        = "test.mp3",
            country         =  defaultCountry,
            language        = language
        )
        val updatedDiscussion = Discussion.reply(
            userId          = userY.id,
            discussionId    = discussion.id,
            duration        = 30,
            fileName        = "test.mp3",
            language        = language
        )
        val linkUrl: String? = TestUtils.getLinkFromEmail(s3 = s3, emailBucket = applicationProperties.emailBucket,
            "click here -> <a href=\"",
            "\">Unsubscribe</a></div>"
        )
        assertNotNull(linkUrl)
        val page:HtmlPage = webClient.getPage(linkUrl)
        Thread.sleep(2000)
        assertEquals("Notification settings", page.titleText)
        assertTrue(page.baseURL.toString().indexOf("/unsubscribe-email") != -1)

        val submitButton = TestUtils.getElementById(page, "submit-button") as HtmlSubmitInput
        assertEquals("Enable", submitButton.valueAttribute)
        userX = User.findById(userX.id)!!
        assertTrue(TestUtils.getEntityBoolean("emailDisabled", userX))

        Discussion.reply(
            userId          = User.createNewUser().id,
            discussionId    = updatedDiscussion.id,
            duration        = 30,
            fileName        = "test.mp3",
            language        = language
        )
        /** Verify email NOT sent **/
        assertEquals(0, TestUtils.numEmailsPresent(s3 = s3, emailBucket = applicationProperties.emailBucket))
    }

    fun loginToUseRootUserOnFrontEnd() : HtmlPage {
        val logoutPage: HtmlPage          = webClient.getPage("$URL/bloip-logout")
        Thread.sleep(1000)
        var page: HtmlPage                = webClient.getPage("$URL/bloip-settings")
        Thread.sleep(1000)
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