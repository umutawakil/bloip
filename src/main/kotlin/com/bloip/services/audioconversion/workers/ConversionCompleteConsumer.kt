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
class ConversionCompleteConsumer(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val commentService: CommentService,
    @Autowired private val discussionService: DiscussionService
) {

    private lateinit var sqsClient: AmazonSQSAsync

    @PostConstruct
    fun init() {
        if (applicationProperties.enableRemoteServices == Constants.REMOTE_SERVICES_ON) {
            loggingService.log("ConversionCompleteConsumer fully initialized and will make REAL remote calls!!!")

            sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
                applicationProperties.awsUploadAccessKey,
                applicationProperties.awsUploadSecretKey
            ).build()

            WorkerUtils.runInLoop(
                "ConversionComplete",
                loggingService = loggingService,
                audioConversionConsumerThreadSleepMillis = applicationProperties.audioConversionConsumerThreadSleepMillis
            ) {
                conversionCompleteConsumerRead()
            }

        } else {
            loggingService.log("ConversionCompleteConsumer NOT fully initialized and will make FAKE remote calls!!!")
        }
    }

    /** conversionCompleteConsumer code **/
    fun conversionCompleteConsumerRead() {
        WorkerUtils.readFromQueue(
            queryUrl = applicationProperties.conversionCompleteQueueUrl,
            sqsClient = sqsClient,
            maxAudioQueueBatchSize = applicationProperties.maxAudioQueueBatchSize,
            messageToComment = { message: Message ->
                commentService.getByJobId(message.body)
            },
            callback = { comment: Comment, discussion: Discussion ->
                comment.audioConversionInProgress = false
                comment.needsConversion = false
                commentService.save(comment)

                //if (discussion != null) {
                discussion.needsConversion = false
                discussion.audioConversionInProgress = false
                discussionService.update(discussion)
               // }
            },
            discussionService = discussionService,
            loggingService = loggingService
        )
    }
}