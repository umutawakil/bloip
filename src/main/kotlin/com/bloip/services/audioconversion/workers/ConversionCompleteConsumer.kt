package com.bloip.services.audioconversion.workers

import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.model.Message
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
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
    @Autowired private val discussionService: DiscussionService
) {

    private lateinit var sqsClient: AmazonSQSAsync

    @PostConstruct
    fun init() {
        loggingService.log("ConversionCompleteConsumer initializing...")
        sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
            applicationProperties.awsUploadAccessKey,
            applicationProperties.awsUploadSecretKey
        ).build()
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            loggingService.log("initialized: RemoteServices: ${applicationProperties.enableRemoteServices}")
            return
        }
        WorkerUtils.runInLoop(
            "ConversionComplete",
            loggingService = loggingService,
            threadSleepMillis = applicationProperties.audioConversionConsumerThreadSleepMillis
        ) {
            conversionCompleteConsumerRead()
        }
        loggingService.log("initialized: RemoteServices: ${applicationProperties.enableRemoteServices}")
    }

    /** conversionCompleteConsumer code **/
    fun conversionCompleteConsumerRead() {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }

        WorkerUtils.readFromQueue(
            queryUrl = applicationProperties.conversionCompleteQueueUrl,
            sqsClient = sqsClient,
            maxAudioQueueBatchSize = applicationProperties.maxAudioQueueBatchSize,
            forEachMessage = { message: Message ->
                val pair: Pair<Int, Discussion>? = discussionService.getByJobInfo(jobId = message.body)
                if(pair == null) {
                    loggingService.log("Unable to locate job by Id")
                } else {
                    discussionService.conversionComplete(discussion = pair.second, trackNumber = pair.first)
                }
            },
            loggingService = loggingService
        )
    }
}