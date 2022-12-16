package com.bloip.services.admin

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.bloip.configuration.ApplicationProperties
import com.bloip.msc.Constants
import com.bloip.services.LoggingService
import com.bloip.services.audioconversion.workers.WorkerUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/19/22.
 */
@Component
class AdminService(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService
) {
    private var errors:MutableSet<String> = HashSet<String>()

    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )
    private lateinit var sqsClient: AmazonSQSAsync
    private lateinit var snsClient: AmazonSNSAsync

    private val EVENT_NOTIFICATION_INTERVAL = applicationProperties.eventNotificationInterval
    private var eventNotificationCount      = 0
    private var ERROR_SUPPRESSION_SIZE      = 25
    private var errorSuppressionCount       = 0

    @PostConstruct
    fun init() {
        loggingService.log("AdminNotifier initializing...")

        sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
            applicationProperties.awsUploadAccessKey,
            applicationProperties.awsUploadSecretKey
        ).build()

        snsClient =  AmazonSNSAsyncClient.asyncBuilder().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )).build()

        loggingService.log("AdminNotifier Service initialized: Remote Services: ${applicationProperties.enableRemoteServices}")
    }

    fun recordException(exception: Exception) {
        if(applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) return

        executorService.execute {
            processExceptionHelper(exception)
        }
    }

    /** ONly notify on new errors by restart the buffering after X ignored already seen errors **/
    private fun processExceptionHelper(exception: Exception)  {
        if (errorSuppressionCount >= ERROR_SUPPRESSION_SIZE) {
            errors.clear()
            errorSuppressionCount = 0
        }
        val message: String = exception.message ?: exception.stackTraceToString()
        if (!errors.contains(message)) {
            errors.add(message)
            loggingService.error("Exception caught by global handler", exception = exception)
            notifyError(message = exception.stackTraceToString())
        }
        errorSuppressionCount++
        addToErrorQueue(exception.stackTraceToString())
    }

    private fun addToErrorQueue(stackTrace: String) {
        val sqsMessage = SendMessageRequest()
            .withQueueUrl(applicationProperties.errorQueueUrl)
            .withMessageBody(stackTrace)
            .withMessageGroupId("errorQueue" + applicationProperties.environment)
        sqsClient.sendMessage(sqsMessage)
    }

    /** This is for general site processes such as newly created discussions and replies. At the time of this writing
     * All new discussions and replies are queued by this handler and each one triggers a direct email to the admin topic
     * but as number of events grows the email notification frequency will probably need some sort of linear relationship
     * for x number of events.
     *
     * */
    fun recordEvent(eventMessage: String) {
        if(applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) return

        executorService.execute {
            processEventHelper(eventMessage)
        }
    }

    private fun processEventHelper(messageMessage: String)  {
        addToAdminQueue(messageMessage)
        eventNotificationCount++

        if (eventNotificationCount < EVENT_NOTIFICATION_INTERVAL) {
            return
        }
        eventNotificationCount = 0
        notifyAdmin(message = messageMessage)
    }

    private fun addToAdminQueue(message: String) {
        val sqsMessage = SendMessageRequest()
            .withQueueUrl(applicationProperties.eventQueueUrl)
            .withMessageBody(message)
            .withMessageGroupId("eventQueue" + applicationProperties.environment)
        sqsClient.sendMessage(sqsMessage)
    }

    private fun notifyError(message: String?) {
        snsClient.publish(applicationProperties.errorTopic, message)

        //TODO: Email admins
    }

    private fun notifyAdmin(message: String?) {
        snsClient.publish(applicationProperties.eventTopic, message)

        //TODO: Email admins
    }
}