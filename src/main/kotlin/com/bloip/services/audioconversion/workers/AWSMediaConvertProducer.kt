package com.bloip.services.audioconversion.workers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsync
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClient
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClientBuilder
import com.amazonaws.services.mediaconvert.model.*
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 10/26/22.
 */
@Component
class AWSMediaConvertProducer(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val discussionService: DiscussionService,
) {

    private lateinit var mediaConverterClient: AWSMediaConvertAsync
    private val executorService: ExecutorService = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue<Runnable>()
    )

    @PostConstruct
    fun init() {
        loggingService.log("AWSMediaConvertProducer initializing...")
        mediaConverterClient = buildAWSMediaClientBuilder().withEndpointConfiguration(
            AwsClientBuilder.EndpointConfiguration(
                getMediaConvertEndpointUrl(), applicationProperties.awsUploadRegion
            )
        ).build()
        loggingService.log("initialized: RemoteServices: ${applicationProperties.enableRemoteServices}")
    }

    //TODO: This is a ridiculous way to get the endpoint URL but that is all I've found so far in the SDK examples
    private fun getMediaConvertEndpointUrl(): String {
        val endPointUrl = buildAWSMediaClientBuilder().build().describeEndpoints(DescribeEndpointsRequest()).endpoints[0].url
        loggingService.log("MediaConvert endpoint url -> $endPointUrl")

        return endPointUrl
    }

    private fun buildAWSMediaClientBuilder(): AWSMediaConvertAsyncClientBuilder {
        return AWSMediaConvertAsyncClient.asyncBuilder().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
                )
            )
        )
    }

    fun sendAWSMediaConverterRequest(discussion: Discussion, trackNumber: Int) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        executorService.execute {
            sendAWSMediaConverterRequestHelper(discussion = discussion, trackNumber = trackNumber)
        }
    }

    private fun sendAWSMediaConverterRequestHelper(discussion: Discussion, trackNumber: Int) {
        mediaConverterClient.createJobAsync(
            CreateJobRequest()
                .withAccelerationSettings(AccelerationSettings().withMode("DISABLED"))
                .withStatusUpdateInterval(StatusUpdateInterval.SECONDS_60)
                .withPriority(0)
                .withHopDestinations()
                .withSettings(
                    discussion.createJobSettings(
                        awsUploadBucketName = applicationProperties.awsUploadBucketName,
                        trackNumber         = trackNumber
                    )
                )
                .withRole(applicationProperties.mediaConvertRole),
            discussionService.buildMediaConvertHandler(
                discussion        = discussion,
                trackNumber       = trackNumber,
                discussionService = discussionService,
                loggingService    = loggingService
            )
        )
    }
}