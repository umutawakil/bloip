package com.bloip.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClientBuilder
import com.amazonaws.services.simpleemail.model.*
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.value.EmailAddress
import com.bloip.services.admin.AdminService
import com.bloip.services.audioconversion.workers.WorkerUtils
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.*
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/25/22.
 */
@Service
class EmailService(
    @Autowired val adminService: AdminService,
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService
) {

    private lateinit var sesClient: AmazonSimpleEmailServiceAsync
    private lateinit var sqsClient: AmazonSQSAsync

    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    //TODO: Needs translation keys

    @PostConstruct
    fun init() {
        loggingService.log("Initializing email service....")
        sesClient = AmazonSimpleEmailServiceAsyncClientBuilder.standard().
        withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        ).build()

        sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
            applicationProperties.awsUploadAccessKey,
            applicationProperties.awsUploadSecretKey
        ).build()

        WorkerUtils.runInLoop(
            threadName        = "EmailService",
            loggingService    = loggingService,
            threadSleepMillis = applicationProperties.emailQueuePollInterval
        ) {
            readFromQueueAndSendEmails()
        }
        loggingService.log("Email service initialized!!!")
    }

    fun sendAccountConfirmationToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/complete-signup?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendPasswordResetToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/bloip-reset-my-password?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Reset my password",
            body      = "Click this link to reset your password <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendEmailResetToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/bloip-reset-my-email?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendDiscussionNotification(token: String, toAddress: EmailAddress) {
        var inboxUrl           = applicationProperties.baseUrl + "/inbox"
        var tokenUrl: String   = applicationProperties.baseUrl + "/unsubscribe-email?t=$token"
        var mainMessage        = "<div>Check your inbox to see new messages -> <a href=\"$inboxUrl\"> My Inbox </a></div>"
        var unsubscribeMessage = "<div>To <a href=\"$tokenUrl\">unsubscribe</a> from these emails click here -> <a href=\"$tokenUrl\">Unsubscribe</a></div>"

        send(
            toAddress = toAddress,
            subject   = "Someone has replied in one of your discussions.",
            body      = "$mainMessage<BR/><BR/><BR/>$unsubscribeMessage"
        )
    }

    private fun send(toAddress: EmailAddress, subject: String, body: String) {
        executorService.execute {
            try {
                val jsonObject = JSONObject()
                jsonObject.put("to", toAddress.value)
                jsonObject.put("subject", subject)
                jsonObject.put("body", body)

                val sqsMessage = SendMessageRequest()
                    .withQueueUrl(applicationProperties.emailQueueUrl)
                    .withMessageBody(jsonObject.toString())
                    .withMessageGroupId("email-" + applicationProperties.environment)
                sqsClient.sendMessage(sqsMessage)

            } catch (exception: Exception) {
                exception.printStackTrace()
                adminService.recordException(exception = exception)
            }
        }
    }

    private fun sendHelper(toAddress: EmailAddress, subject: String, body: String) {
        sesClient.sendEmail(
            SendEmailRequest(
                "Bloip <${applicationProperties.serviceEmailAddress}>",
                Destination(listOf(toAddress.value)),
                Message(
                    Content(subject).withCharset(Charsets.UTF_8.name()),
                    Body().withHtml(Content(body).withCharset(Charsets.UTF_8.name()))
                )
            )
        )
    }

    private fun readFromQueueAndSendEmails() {
        val messageResult: Future<ReceiveMessageResult> = sqsClient.receiveMessageAsync(
            ReceiveMessageRequest().
            withQueueUrl(applicationProperties.emailQueueUrl).
            withMaxNumberOfMessages(1)
        )
        val receiveMessageResult: ReceiveMessageResult = messageResult.get()
        loggingService.log("Email queue message count: "+receiveMessageResult.messages.size)
        for (m in receiveMessageResult.messages) {

            val jsonObject = JSONObject(m.body)
            sendHelper(
                toAddress = EmailAddress(jsonObject.get("to") as String),
                subject   = jsonObject.get("subject") as String,
                body      = jsonObject.get("body") as String
            )
            sqsClient.deleteMessageAsync(applicationProperties.emailQueueUrl, m.receiptHandle)
        }
    }
}