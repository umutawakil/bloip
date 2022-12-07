package com.bloip.integration

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3Object
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.User
import com.bloip.services.UserService
import com.bloip.services.UserTokenService
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * Created by Usman Mutawakil on 12/5/22.
 *
 * These tests validate real HTTP requests using a headless browser driver to simulate clicks on the web page.
 * It tests the UI flows for user signup, login, forgot password, change email, etc. It generates real emails
 * and reads them by having these test email addresses forward their emails to an s3 bucket which can be read easily
 * programmatically.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    properties = ["server.port=8443"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserAccountEndToEndFunctionalTests (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val userService: UserService,
    @Autowired private val userTokenService: UserTokenService
){
    private lateinit var webClient: WebClient
    private val URL = "https://localhost:8443"
    private val TEST_USER_1_USERNAME = "test1@dev.bloip.com"
    private val TEST_NEW_USERNAME    = "test2@dev.bloip.com"
    private val GOOD_PASSWORD = "sfsfsfsfsfdfdfdf"

    private lateinit var s3: AmazonS3

    @BeforeAll
    fun init() {
        webClient = WebClient()
        webClient.options.isCssEnabled = false
        webClient.options.isJavaScriptEnabled = true
        webClient.options.isUseInsecureSSL = true

        s3 = AmazonS3ClientBuilder.standard(). withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        ).build()

        clearEmailBucket()
        deleteByUsername(TEST_USER_1_USERNAME)
        deleteByUsername(TEST_NEW_USERNAME)
    }

    @AfterAll
    fun teardown() {
        webClient.close()
        userTokenService.clearAll()
        clearEmailBucket()
        deleteByUsername(TEST_USER_1_USERNAME)
        deleteByUsername(TEST_NEW_USERNAME)
    }

    fun clearEmailBucket() {
        val objectList = s3.listObjects(applicationProperties.emailBucket)
        for(s in objectList.objectSummaries) {
            s3.deleteObject(applicationProperties.emailBucket, s.key)
        }
    }
    fun deleteByUsername(username: String) {
        val user: User? = userService.findByUsername(username)
        if (user != null) {
            userService.delete(user.id)
        }
    }

    @Test
    @Order(0)
    fun Can__Submit__signup__form() {
        var page:HtmlPage = webClient.getPage("$URL/bloip-login")
        val signupLink = getElementById(page,"signup") as HtmlAnchor
        page = signupLink.click()

        /** Verify the correct page **/
        var s = SignupSubmission(page)

        /** Verify can't submit garbage **/
        page = s.send("garbage", "garbage", true) //bad input
        s = SignupSubmission(page)
        assertNull(getElementById(page,"success"))

        page = s.send(TEST_USER_1_USERNAME, "test2@dev.bloip.com", true)
        assertNull(getElementById(page,"success"))

        page = s.send(TEST_USER_1_USERNAME, TEST_USER_1_USERNAME, false)
        assertNull(getElementById(page,"success"))

        /**Verify can't choose an already taken email address**/
        page = s.send(applicationProperties.shogunUsername, applicationProperties.shogunUsername, true)
        assertNull(getElementById(page,"success"))
        assertNotNull(getElementById(page,"error"))

        /** Verify valid input triggers an email/success response (email is verified in the subsequent steps). **/
        page = s.send(TEST_USER_1_USERNAME, TEST_USER_1_USERNAME, true)
        assertNotNull(getElementById(page,"success"))
    }

    fun getElementById(page: HtmlPage, id: String) : DomElement? {
        return try {
            page.getHtmlElementById(id)
        } catch (e: Exception) {
            null
        }
    }

    class SignupSubmission {
        val titleText = "Create an account"
        val email1Field:   HtmlEmailInput
        val email2Field:   HtmlEmailInput
        val adultCheckbox: HtmlCheckBoxInput
        val submitButton:  HtmlSubmitInput

        constructor(page: HtmlPage) {
            assertEquals(titleText, page.titleText)
            this.email1Field   = page.getHtmlElementById("email1")
            this.email2Field   = page.getHtmlElementById("email2")
            this.adultCheckbox = page.getHtmlElementById("adult-checkbox")
            this.submitButton  = page.getHtmlElementById("submit-button")
        }

        fun send(val1: String, val2: String, checkBox: Boolean) :  HtmlPage {
            email1Field.valueAttribute = val1
            email2Field.valueAttribute = val2
            adultCheckbox.isChecked = checkBox
            return submitButton.click()
        }
    }

    @Test
    @Order(1)
    fun Can__complete__signup__form() {
        val link = getSignupFromEmailLink()

        /** Go to the link in the email and assert everything is as expected **/
        println("Signup Link in email: $link")
        var page:HtmlPage = webClient.getPage(link)
        assertNull(getElementById(page,"error-1"))
        assertNull(getElementById(page,"error-2"))
        assertNotNull(getElementById(page,"no-error-container"))
        assertEquals("Finish account", page.titleText)

        /** Get control buttons **/
        val password: HtmlPasswordInput = getElementById(page,"password") as HtmlPasswordInput
        val submitButton: HtmlSubmitInput = getElementById(page, "submit-button") as HtmlSubmitInput

        /** Short password test **/
        password.valueAttribute = "XXXX"
        page = submitButton.click()
        assertEquals("Finish account", page.titleText)

        /** Confirm a good password results in success **/
        password.valueAttribute = GOOD_PASSWORD
        page = submitButton.click()
        assertEquals("Account created", page.titleText)

        /** Confirm going to the token/link again shows the expired link **/
        page = webClient.getPage(link)
        assertNotNull(getElementById(page,"error-1"))
        assertNull(getElementById(page,"no-error-container"))
        assertEquals("Finish account", page.titleText)
    }

    fun getSignupFromEmailLink() : String {
        return getLinkFromEmail(
            "Click this link to confirm your email address <a href=\"",
        "\"> Click here </a>"
        )
    }

    fun getLinkFromEmail(beforeSearchText: String, afterSearchText: String) : String {
        /** Get the token out of the s3 bucket and assert that it's present/ grab the link to the token**/
        var checks = 0
        while(s3.listObjects(applicationProperties.emailBucket).objectSummaries.size ==0 && checks < 10) {
            Thread.sleep(1000)
            checks++
        }
        val objectList = s3.listObjects(applicationProperties.emailBucket)
        var s3Object: S3Object? = null
        for(s in objectList.objectSummaries) {
            s3Object = s3.getObject(applicationProperties.emailBucket, s.key)
        }
        println("ObjectList Size: " + objectList.objectSummaries.size)
        println("Object: " + s3Object!!.key)

        val lines: List<String>  = s3Object.objectContent.bufferedReader().lines().toList()
        s3.deleteObject(applicationProperties.emailBucket, s3Object.key) //Causes problems in subsequent emails if this sticks around

        return lines[lines.size - 1].
        replace(beforeSearchText, "").
        replace(afterSearchText, "")
    }


    @Test
    @Order(2)
    fun Can__login__with__new__credentials() {
        var page:HtmlPage = webClient.getPage("$URL/bloip-settings")
        assertEquals("Login", page.titleText)
        assertNull(getElementById(page,"error"))

        val email: HtmlEmailInput         = getElementById(page,"email") as HtmlEmailInput
        val password: HtmlPasswordInput   = getElementById(page,"password") as HtmlPasswordInput
        val submitButton: HtmlSubmitInput = getElementById(page,"submit-button") as HtmlSubmitInput

        /** Verify user can't enter with bad input **/
        email.valueAttribute    = "testX@dev.bloip.com"
        password.valueAttribute = "xfsfs"
        page = submitButton.click()
        assertEquals("Login", page.titleText)
        assertNotNull(getElementById(page,"error"))

        /** Verify user can enter with valid input **/
        email.valueAttribute    = TEST_USER_1_USERNAME
        password.valueAttribute = GOOD_PASSWORD
        page = submitButton.click()
        assertEquals("Settings", page.titleText)
    }

    @Test
    @Order(3)
    fun Can__logout() {
        var page:HtmlPage = webClient.getPage("$URL/bloip-logout")
        assertEquals("Login", page.titleText)

        page = webClient.getPage("$URL/bloip-settings")
        assertEquals("Login", page.titleText)
    }


    @Test
    @Order(4)
    fun Can__use__forgot__password() {
        var page:HtmlPage = webClient.getPage("$URL/bloip-login")
        assertEquals("Login", page.titleText)

        val link: HtmlAnchor = getElementById(page,"forgot-my-password") as HtmlAnchor
        page = link.click()
        assertEquals("Forgot my password", page.titleText)
        assertNull(getElementById(page, "error"))
        assertNull(getElementById(page, "success"))

        /** Ensure an error prompt is presented if the email address doesn't exist **/
        var email: HtmlEmailInput = getElementById(page,"email") as HtmlEmailInput
        var submitButton: HtmlSubmitInput = getElementById(page,"submit-button") as HtmlSubmitInput

        email.valueAttribute = "sfsfsf@dev.bloip.com"
        page = submitButton.click()
        assertEquals("Forgot my password", page.titleText)
        assertNotNull(getElementById(page, "error"))

        /** Ensure a valid email triggers a success **/
        email = getElementById(page,"email") as HtmlEmailInput
        submitButton = getElementById(page,"submit-button") as HtmlSubmitInput
        email.valueAttribute = TEST_USER_1_USERNAME
        page = submitButton.click()

        assertEquals("Forgot my password", page.titleText)
        assertNull(getElementById(page, "error"))
        assertNotNull(getElementById(page, "success"))

        /** Verify you can navigate to the reset completion page from the email **/
        val resetLink = getPasswordResetLinkFromEmailLink()
        page = webClient.getPage(resetLink)
        assertEquals("Reset password", page.titleText)
        assertNull(getElementById(page, "error"))

        /** Verify BS password input is rejected **/
        var passwordInput1 = getElementById(page, "password") as HtmlPasswordInput
        var passwordInput2 = getElementById(page, "password2") as HtmlPasswordInput
        submitButton       = getElementById(page, "submit-button") as HtmlSubmitInput

        passwordInput1.valueAttribute = "XX"
        passwordInput2.valueAttribute = "88888*******"
        page = submitButton.click()
        assertEquals("Reset password", page.titleText)

        /** Verify valid input is accepted and user is redirected to login page **/
        passwordInput1 = getElementById(page, "password") as HtmlPasswordInput
        passwordInput2 = getElementById(page, "password2") as HtmlPasswordInput
        submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        passwordInput1.valueAttribute = GOOD_PASSWORD
        passwordInput2.valueAttribute = GOOD_PASSWORD
        page = submitButton.click()

        assertEquals("Login", page.titleText)
        assertNotNull(getElementById(page, "password-reset"))
        assertNull(getElementById(page, "error"))

        /** User can login **/
        val loginEmail               = getElementById(page,"email") as HtmlEmailInput
        var loginPassword            = getElementById(page,"password") as HtmlPasswordInput
        var loginButton              = getElementById(page,"submit-button") as HtmlSubmitInput
        loginEmail.valueAttribute    = TEST_USER_1_USERNAME
        loginPassword.valueAttribute = GOOD_PASSWORD
        page                         = loginButton.click()
        assertEquals("Settings", page.titleText)

        /** Verify password reset link has expired **/
        val resetPage: HtmlPage = webClient.getPage(resetLink)
        assertEquals("Reset password", resetPage.titleText)
        assertNotNull(getElementById(resetPage, "error"))
    }

    fun getPasswordResetLinkFromEmailLink() : String {
        return getLinkFromEmail(
            "Click this link to reset your password <a href=\"",
            "\"> Click here </a>"
        )
    }

    @Test
    @Order(5)
    fun Can__change__my__email__address() {
        var page:HtmlPage = webClient.getPage("$URL/bloip-settings")
        var emailButton = getElementById(page, "email-settings") as HtmlButton
        page = emailButton.click()
        assertEquals("Change Email Address", page.titleText)

        /** Assert you can not change to an email address already in use **/
        var email = getElementById(page, "newEmail") as HtmlEmailInput
        email.valueAttribute = applicationProperties.shogunUsername
        var submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        page = submitButton.click()
        assertNotNull(getElementById(page,"error"))
        assertNull(getElementById(page, "success"))

        /** Verify can submit with a valid unused username/email **/
        Thread.sleep(1200)
        email                = getElementById(page, "newEmail") as HtmlEmailInput
        submitButton         = getElementById(page, "submit-button") as HtmlSubmitInput
        email.valueAttribute = TEST_NEW_USERNAME
        page = submitButton.click()
        Thread.sleep(1000)
        assertNotNull(getElementById(page,"success"))
        assertNull(getElementById(page, "error"))

        /** Verify you can navigate to the new email address and click the verification link **/
        val link = getEmailChangeConfirmationFromEmail()
        page = webClient.getPage(link)
        assertEquals("Email reset", page.titleText)
        assertNull(getElementById(page, "error"))

        /** Navigate to the link again or refresh the page and confirm its expired **/
        page = webClient.getPage(link)
        assertEquals("Email reset", page.titleText)
        assertNotNull(getElementById(page, "error"))
    }

    fun getEmailChangeConfirmationFromEmail() : String {
        return getLinkFromEmail(
            "Click this link to confirm your email address <a href=\"",
            "\"> Click here </a>"
        )
    }

    @Test
    @Order(6)
    fun Can__change__my__notifications() {
        var page: HtmlPage = webClient.getPage("$URL/bloip-settings")
        var notificationsButton = getElementById(page, "notification-settings") as HtmlButton
        page = notificationsButton.click()
        assertEquals("Notification settings", page.titleText)

        var submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        var user: User = userService.findByUsername(TEST_NEW_USERNAME)!!
        assertFalse(user.emailDisabled)
        assertEquals("Disable", submitButton.valueAttribute)

        page = submitButton.click()
        submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        assertEquals("Enable", submitButton.valueAttribute)

        user = userService.findByUsername(TEST_NEW_USERNAME)!!
        assertTrue(user.emailDisabled)

        page = submitButton.click()
        submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        assertEquals("Disable", submitButton.valueAttribute)

        user = userService.findByUsername(TEST_NEW_USERNAME)!!
        assertFalse(user.emailDisabled)
    }

    @Test
    @Order(7)
    fun Can__delete__my__account() {
        var page: HtmlPage = webClient.getPage("$URL/bloip-settings")
        var button = getElementById(page, "delete-my-account") as HtmlButton
        page = button.click()
        assertEquals("Confirm account deletion", page.titleText)

        /** Confirm you want to delete your account and go to the next page **/
        var link = getElementById(page, "delete-my-account") as HtmlAnchor
        page = link.click()
        assertEquals("Delete my account", page.titleText)

        /** Click the account deletion button **/
        var user: User = userService.findByUsername(TEST_NEW_USERNAME)!!
        assertNotNull(user)
        var submitButton = getElementById(page, "submit-button") as HtmlSubmitInput
        page = submitButton.click()

        /** Confirm we are at the login page, user nolonger exists and can not log in **/
        assertEquals("Login", page.titleText)
        var user2: User? = userService.findByUsername(TEST_NEW_USERNAME)
        assertNull(user2)

        var email = getElementById(page,"email") as HtmlEmailInput
        var password = getElementById(page,"password") as HtmlPasswordInput
        submitButton = getElementById(page,"submit-button") as HtmlSubmitInput
        email.valueAttribute = TEST_NEW_USERNAME
        password.valueAttribute = GOOD_PASSWORD
        page = submitButton.click()

        assertEquals("Login", page.titleText)
        assertNotNull(getElementById(page,"error"))
    }

    //TODO: Signup token limit
    //TODO: Forgot my password token limit
    //TODO: Change my email address token limit
}