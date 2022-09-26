package com.bloip.services.webpush

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.webpush.WebPushNotificationStat
import com.bloip.domain.webpush.WebPushSubscription
import com.bloip.jobs.webpush.ThirdPartyWebPushService
import com.bloip.services.LoggingService
import com.bloip.services.webpush.utils.TestableTimeHelper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import javax.annotation.PostConstruct


/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Service
class WebPushService(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val webPushSubscriptionService: WebPushSubscriptionService,
    @Autowired private val webPushNotificationStatService: WebPushNotificationStatService,
    @Autowired private val loggingService: LoggingService
) {

    @Autowired var pushService: ThirdPartyWebPushService? = null

    var testableTimeHelper: TestableTimeHelper = TestableTimeHelper()

    fun deleteAll() {
        webPushNotificationStatService.deleteAll()
        webPushSubscriptionService.deleteAll()
    }

    @Transactional
    fun saveNewSubscription(userId: Long, privateKey: String, auth: String, endpoint: String, expirationTime: String?) {
        webPushSubscriptionService.save(
            userId         = userId,
            privateKey     = privateKey,
            auth           = auth,
            endpoint       = endpoint,
            expirationTime = expirationTime
        )
        webPushNotificationStatService.createNewStat(userId = userId)
    }

    fun getWebPushSubscription(userId: Long) : List<WebPushSubscription> {
        return webPushSubscriptionService.getUserSubscriptions(userId = userId)
    }

    fun getWebPushNotificationStat(userId: Long) : WebPushNotificationStat? {
        return webPushNotificationStatService.get(userId =  userId)
    }

    fun scheduleWebPush(userIds: Collection<Long>) {
        webPushNotificationStatService.scheduleWebPush(userIds = userIds)
    }

    fun sendAllPendingWebPushNotifications() {
        for(stat in webPushNotificationStatService.getAllThatNeedNotification()) {
            //If count is 0 increment and send
            if (stat.dailyCount == 0) {
                sendWebPushNotifications(stat)
                continue
            }

            //If time > 24 reset to 1 and send
            if(isCountWindowPassed(y = testableTimeHelper.getCurrentTimeMillis(), x = stat.lastUpdateTimestamp)) {
                stat.dailyCount = 0
                sendWebPushNotifications(stat)
                continue
            }

            //Has the user been notified at the max level already? If so ignore this.
            if (stat.dailyCount > applicationProperties.webPushDailyCountMax) {
                continue
            }

            //If the minimum required amount of time between the last reply has not been reached then ignore
            if(minimumMinuteThreshHoldNOTReached(y = testableTimeHelper.getCurrentTimeMillis(), x = stat.lastUpdateTimestamp)) {
                continue
            }
            sendWebPushNotifications(stat)
        }
    }

    private fun sendWebPushNotifications(stat: WebPushNotificationStat) {
        for(sub in webPushSubscriptionService.getUserSubscriptions(userId = stat.userId)) {
            sendWebPushNotification(sub)
            webPushNotificationStatService.markNotificationStatAsProcessed(stat)
        }
    }

    private fun sendWebPushNotification(webPushSubscription: WebPushSubscription) {
        pushService!!.send(webPushSubscription)
    }

    private fun isCountWindowPassed(y: Timestamp, x: Timestamp) : Boolean {
        val diffMillis = (y.time - x.time)
        val hourDiff = (diffMillis / 1000) / 3600 //Number of hours between them

        return hourDiff >= applicationProperties.webPushCountWindowHours
    }

    private fun minimumMinuteThreshHoldNOTReached(y: Timestamp, x: Timestamp) : Boolean {
        val diffMillis = (y.time - x.time)
        val diffMins = (diffMillis / 1000) / 60 //Number of mins between them

        return diffMins < applicationProperties.webPushMinimumResponseDelayMin
    }
}