package com.bloip.services.audioconversion.workers

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.LoggingService
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.*
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 10/23/22.
 */
@Component
class ConversionRequestProducer (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
) {

    private val producerExecutorService: ExecutorService = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )
    private lateinit var sqsClient: AmazonSQSAsync

    @PostConstruct
    fun init() {
        loggingService.log("ConversionRequestProducer initializing")
        sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
            applicationProperties.awsUploadAccessKey,
            applicationProperties.awsUploadSecretKey
        ).build()
        loggingService.log("ConversionRequestProducer initialized -> RemoteServices: ${applicationProperties.enableRemoteServices}")
    }
    fun startConvertingAudioFile(
        discussionId: Discussion.DiscussionId,
        fileName: String,
        trackNumber: Int
    ) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        producerExecutorService.execute {
            enqueueConversionRequestHelper(
                discussionId = discussionId,
                fileName     = fileName,
                trackNumber  = trackNumber
            )
        }
    }
    private fun enqueueConversionRequestHelper(
        discussionId: Discussion.DiscussionId,
        fileName: String,
        trackNumber: Int
    ) {
        val o = JSONObject()
        o.put("discussionId", discussionId.toString())
        o.put("trackNumber", trackNumber)
        o.put("fileName", fileName)

        val sqsMessage            = SendMessageRequest()
        sqsMessage.queueUrl       = applicationProperties.needsConversionQueueUrl
        sqsMessage.messageBody    = o.toString()
        sqsMessage.messageGroupId = "needsConversion" + applicationProperties.environment
        sqsClient.sendMessage(sqsMessage)
    }
}