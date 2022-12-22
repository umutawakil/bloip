package com.bloip.services.audioconversion.workers

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 10/26/22.
 */
@Component
class ConversionRequestConsumer (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val commentService: CommentService,
    @Autowired private val discussionService: DiscussionService,
    @Autowired private val awsMediaConvertProducer: AWSMediaConvertProducer
)  {

    private lateinit var sqsClient: AmazonSQSAsync

    @PostConstruct
    fun init() {
        loggingService.log("ConversionRequestConsumer initializing...")

        sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
            applicationProperties.awsUploadAccessKey,
            applicationProperties.awsUploadSecretKey
        ).build()
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            loggingService.log("initialized: RemoteServices: ${applicationProperties.enableRemoteServices}")
            return
        }
        WorkerUtils.runInLoop(
            "ConversionRequest",
            loggingService = loggingService,
            threadSleepMillis = applicationProperties.audioConversionConsumerThreadSleepMillis
        ) {
            conversionRequestConsumerRead()
        }
        loggingService.log("initialized: RemoteServices: ${applicationProperties.enableRemoteServices}")
    }

    private fun conversionRequestConsumerRead() {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        WorkerUtils.readFromQueue(
            queryUrl = applicationProperties.needsConversionQueueUrl,
            sqsClient = sqsClient,
            maxAudioQueueBatchSize = applicationProperties.maxAudioQueueBatchSize,
            messageToComment = { message: Message ->
                commentService.get(message.body.toLong())
            },
            callback = { comment: Comment, discussion: Discussion? ->
                awsMediaConvertProducer.sendAWSMediaConverterRequest(comment = comment, discussion = discussion)
            },
            discussionService = discussionService,
            loggingService = loggingService
        )
    }
}