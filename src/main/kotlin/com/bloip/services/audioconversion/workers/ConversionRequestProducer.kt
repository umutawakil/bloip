package com.bloip.services.audioconversion.workers

import com.bloip.services.audioconversion.AudioConversionRequestService
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsync
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClient
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClientBuilder
import com.amazonaws.services.mediaconvert.model.*
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.amazonaws.services.sqs.model.SendMessageRequest
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.CommentService
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.util.concurrent.*
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 10/23/22.
 */
@Component
class ConversionRequestProducer (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
) : AudioConversionRequestService {

    private val producerExecutorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )
    private lateinit var sqsClient: AmazonSQSAsync

    @PostConstruct
    fun init() {
        if (applicationProperties.enableRemoteServices == Constants.REMOTE_SERVICES_ON) {
            loggingService.log("ConversionRequestProducer fully initialized and will make REAL remote calls!!!")
            sqsClient = WorkerUtils.buildAmazonSQSClientBuilder(
                applicationProperties.awsUploadAccessKey,
                applicationProperties.awsUploadSecretKey
            ).build()
        } else {
            loggingService.log("ConversionRequestProducer Service NOT fully initialized and will make FAKE remote calls!!!")
        }
    }
    override fun startConvertingAudioFile(comment: Comment) {
        producerExecutorService.execute {
            enqueueConversionRequestHelper(comment = comment)
        }
    }
    private fun enqueueConversionRequestHelper(comment: Comment) {
        val sqsMessage = SendMessageRequest()
            .withQueueUrl(applicationProperties.needsConversionQueueUrl)
            .withMessageBody("${comment.id}")
        sqsClient.sendMessage(sqsMessage)
    }
}