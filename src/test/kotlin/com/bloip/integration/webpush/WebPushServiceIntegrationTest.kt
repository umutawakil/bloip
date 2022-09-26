package com.bloip.integration.webpush

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.User
import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.integration.webpush.mock.MockThirdPartyPushService
import com.bloip.repositories.UserRepository
import com.bloip.services.UserService
import com.bloip.services.webpush.WebPushService
import com.bloip.services.webpush.utils.MockTestableTimeHelper
import com.bloip.services.webpush.utils.TestableTimeHelper
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.Assert

/**
 * Created by Usman Mutawakil on 9/20/22.
 */

@SpringBootTest
class WebPushServiceIntegrationTest(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val userService: UserService,
    @Autowired val userRepository: UserRepository,
    @Autowired val webPushService: WebPushService
) {

    val HOUR_IN_MILLIS = 1000 * 3600
    val DAY_IN_MILLIS = HOUR_IN_MILLIS * 24

    @BeforeEach
    fun setup() {
        /** Cleanup **/
        deleteCertainTables()
        webPushService.deleteAll()
        webPushService.testableTimeHelper = TestableTimeHelper()
    }
    @AfterEach
    fun cleanup() {
        /** Cleanup **/
        deleteCertainTables()
        webPushService.deleteAll()
        webPushService.testableTimeHelper = TestableTimeHelper()
    }

    @Test
    fun can__save__new__web__push__subscription() {
        val user: User = userService.createNewUser()
        webPushService.saveNewSubscription(
            userId = user.id,
            privateKey = "",
            auth = "",
            endpoint = "",
            expirationTime = null
        )
        assertFalse(webPushService.getWebPushSubscription(userId = user.id).isEmpty())
    }

    @Test
    fun zero__count__web__push__schedule__immediately_sends() {
        val user: User = userService.createNewUser()
        createWebSubscription(user)

        webPushService.scheduleWebPush(userIds = mutableListOf(user.id))  // Has no scheduled pushes within last 24 hours
        val prePushState = webPushService.getWebPushNotificationStat(userId = user.id)
        assertEquals(0, prePushState!!.dailyCount)
        assertEquals(true, prePushState!!.needsNotification)

        var mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService
        assertFalse(mockThirdPartyPushService.sent)

        webPushService.sendAllPendingWebPushNotifications()
        assertTrue(mockThirdPartyPushService.sent)
        val webPushNotificationStat: WebPushNotificationStat? = webPushService.getWebPushNotificationStat(userId = user.id)
        Assert.notNull(webPushNotificationStat, "Null notification stat")

        assertEquals(1, webPushNotificationStat!!.dailyCount)
        assertEquals(false, webPushNotificationStat!!.needsNotification)

        //This next call should fail because the minimum delay needed for a reply notification has not been met
        mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService
        webPushService.sendAllPendingWebPushNotifications()
        assertFalse(mockThirdPartyPushService.sent)
    }

    @Test
    fun max_count__web__push__stat__is__ignored__if__day__window__passed() {
        val user: User = userService.createNewUser()
        createWebSubscription(user)

        val mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService

        val mockTimeHelper = MockTestableTimeHelper()
        mockTimeHelper.testTime = System.currentTimeMillis() + (DAY_IN_MILLIS * 2) // 48 hours
        webPushService.testableTimeHelper = mockTimeHelper

        repeat(times = applicationProperties.webPushDailyCountMax) {
            send(userIds = mutableListOf(user.id))
            assertTrue(mockThirdPartyPushService.sent)
        }
        assertEquals(mockThirdPartyPushService.callCount, applicationProperties.webPushDailyCountMax)

        //The next should fail because the daily limit is reached
        val finalMockPush = MockThirdPartyPushService()
        webPushService.pushService = finalMockPush
        webPushService.scheduleWebPush(userIds = mutableListOf(user.id))
        assertFalse(finalMockPush.sent)
    }

    // If time > 24, despite count reached max, confirm formerly blocked messages because of the max are allowed through once 24 hour limit passes.  Confirm reset to 1
    @Test
    fun max_count__reached__but__24__hours__passed() {
        val user: User = userService.createNewUser()
        createWebSubscription(user)

        val mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService

        //set a fake time in the future just to allow the count to hit over the max
        val mockTimeHelper = MockTestableTimeHelper()
        mockTimeHelper.testTime = System.currentTimeMillis() + (DAY_IN_MILLIS * 2) // 48 hours
        webPushService.testableTimeHelper = mockTimeHelper

        repeat(times = applicationProperties.webPushDailyCountMax * 2) {
            send(userIds = mutableListOf(user.id))
            assertTrue(mockThirdPartyPushService.sent)
        }
        assertEquals(applicationProperties.webPushDailyCountMax * 2, mockThirdPartyPushService.callCount)

        //set a fake time in the 'past''just to allow the count to block sending because min threshold is not met
        val mockTimeHelper2 = MockTestableTimeHelper()
        mockTimeHelper2.testTime = System.currentTimeMillis() + applicationProperties.webPushMinimumResponseDelayMin + 1000
        webPushService.testableTimeHelper = mockTimeHelper2

        //Confirm blocked by count but time is sufficient to send but won't
        val finalMockPush = MockThirdPartyPushService()
        webPushService.pushService = finalMockPush
        send(userIds = mutableListOf(user.id))
        assertFalse(finalMockPush.sent)

        //Now confirm it can send ignoring blocks because the 24 hour window is passed.
        mockTimeHelper2.testTime          = System.currentTimeMillis() + (DAY_IN_MILLIS * 2)  // 48 hours
        webPushService.testableTimeHelper = mockTimeHelper2

        send(userIds = mutableListOf(user.id))
        assertTrue(finalMockPush.sent)
        assertTrue(webPushService.getWebPushNotificationStat(userId = user.id)!!.dailyCount == 1)
    }

    //If time stamp is < min_notification_threshold ignore and return
    @Test
    fun ignore__if__minimum__threshold__not__met__send__if__met() {
        val user: User = userService.createNewUser()
        createWebSubscription(user)

        var mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService

        send(userIds = mutableListOf(user.id))
        assertTrue(mockThirdPartyPushService.sent)

        //Should fail since 1 notification was already sent but hasn't been any time since then.
        mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService
        send(userIds = mutableListOf(user.id))
        assertFalse(mockThirdPartyPushService.sent)

        //Now pass the required minimum time and confirm it can send
        val mockTimeHelper = MockTestableTimeHelper()
        mockTimeHelper.testTime = System.currentTimeMillis() + applicationProperties.webPushMinimumResponseDelayMin * (DAY_IN_MILLIS * 2)//System.currentTimeMillis() + (3600 * 24 * 2) // 48 hours
        webPushService.testableTimeHelper = mockTimeHelper

        mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService
        send(userIds = mutableListOf(user.id))
        assertTrue(mockThirdPartyPushService.sent)
    }

    @Test
    fun can__send__multiple__notifications() {
        val userList = mutableListOf<User>()
        repeat(10) {
            userList.add(userService.createNewUser())
        }
        for(user in userList) {
            createWebSubscription(user = user)
        }

        val mockThirdPartyPushService = MockThirdPartyPushService()
        webPushService.pushService = mockThirdPartyPushService

        //set a fake time in the future just to allow the count to hit over the max
        val mockTimeHelper = MockTestableTimeHelper()
        mockTimeHelper.testTime = System.currentTimeMillis() + (DAY_IN_MILLIS * 2) // 48 hours
        webPushService.testableTimeHelper = mockTimeHelper

        repeat(times = 20) {
            send(userIds = userList.map(User::id))
            assertTrue(mockThirdPartyPushService.sent)
        }
        assertEquals(userList.size * 20, mockThirdPartyPushService.callCount)
    }

    fun send(userIds: List<Long>) {
        webPushService.scheduleWebPush(userIds = userIds)
        webPushService.sendAllPendingWebPushNotifications()
    }

    fun createWebSubscription(user: User) {
        webPushService.saveNewSubscription(
            userId = user.id,
            privateKey = "",
            auth = "",
            endpoint = "",
            expirationTime = null
        )
    }

    fun deleteCertainTables() {
        userRepository.deleteAll()
    }
}