package com.bloip.services.audioconversion.workers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsync
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClient
import com.amazonaws.services.mediaconvert.AWSMediaConvertAsyncClientBuilder
import com.amazonaws.services.mediaconvert.model.*
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.CommentService
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
    @Autowired private val commentService: CommentService,
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

    fun sendAWSMediaConverterRequest(comment: Comment, discussion: Discussion?) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        executorService.execute {
            sendAWSMediaConverterRequestHelper(comment = comment, discussion = discussion)
        }
    }

    private fun sendAWSMediaConverterRequestHelper(comment: Comment, discussion: Discussion?) {
        mediaConverterClient.createJobAsync(
            CreateJobRequest()
                .withAccelerationSettings(AccelerationSettings().withMode("DISABLED"))
                .withStatusUpdateInterval(StatusUpdateInterval.SECONDS_60)
                .withPriority(0)
                .withHopDestinations()
                .withSettings(createJobSettings(keyName = comment.fileName))
                .withRole(applicationProperties.mediaConvertRole),
            MediaConvertHandler(
                discussion = discussion,
                comment = comment,
                discussionService = discussionService,
                commentService = commentService,
                loggingService = loggingService
            )
        )
    }

    private fun createJobSettings(keyName: String): JobSettings {
        val jobSettings = JobSettings()

        jobSettings.timecodeConfig = TimecodeConfig().withSource("ZEROBASED")

        val outputGroup = OutputGroup()
        outputGroup.name = "file-group-1"
        outputGroup.customName = "File Group"

        val audioDescription = AudioDescription()
        audioDescription.audioSourceName = "Audio Selector 1"
        audioDescription.codecSettings = AudioCodecSettings()
        audioDescription.codecSettings.codec = "AAC"
        audioDescription.codecSettings.aacSettings = AacSettings()
        audioDescription.codecSettings.aacSettings.bitrate = 96000
        audioDescription.codecSettings.aacSettings.codingMode = "CODING_MODE_2_0"
        audioDescription.codecSettings.aacSettings.sampleRate = 48000
        audioDescription.codecSettings.aacSettings.specification = "MPEG4"

        var output = Output()
        output.containerSettings = ContainerSettings()
        output.containerSettings.container = "MP4"
        output.setAudioDescriptions(listOf(audioDescription))
        outputGroup.setOutputs(listOf(output))

        outputGroup.outputGroupSettings = OutputGroupSettings()
        outputGroup.outputGroupSettings.type = "FILE_GROUP_SETTINGS"
        outputGroup.outputGroupSettings.fileGroupSettings = FileGroupSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destination =
            "s3://${applicationProperties.awsUploadBucketName}/output/"
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings = DestinationSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings = S3DestinationSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings.accessControl =
            S3DestinationAccessControl()
        outputGroup.outputGroupSettings.fileGroupSettings.destinationSettings.s3Settings.accessControl.cannedAcl =
            "PUBLIC_READ"
        jobSettings.setOutputGroups(listOf(outputGroup))

        val input = Input()
        val audioSelector = AudioSelector()
        audioSelector.defaultSelection = "DEFAULT"
        audioSelector.externalAudioFileInput = "s3://${applicationProperties.awsUploadBucketName}/$keyName"
        //audioSelector.audioDurationCorrection    = "AUTO"
        input.audioSelectors = HashMap<String, AudioSelector>()
        input.audioSelectors["Audio Selector 1"] = audioSelector
        input.timecodeSource = "ZEROBASED"

        jobSettings.setInputs(listOf(input))

        return jobSettings
    }

    private class MediaConvertHandler : AsyncHandler<CreateJobRequest, CreateJobResult> {
        private val discussionService: DiscussionService
        private val commentService: CommentService
        private val discussion: Discussion?
        private val comment: Comment
        private val loggingService: LoggingService

        constructor(
            discussion: Discussion?,
            comment: Comment,
            discussionService: DiscussionService,
            commentService: CommentService,
            loggingService: LoggingService
        ) {
            this.discussion = discussion
            this.comment = comment
            this.discussionService = discussionService
            this.commentService = commentService
            this.loggingService = loggingService
        }

        override fun onError(exception: Exception) {
            loggingService.error("Error sending request to AWS Media convert", exception)
        }

        override fun onSuccess(request: CreateJobRequest, result: CreateJobResult) {
            if (discussion != null) {
                discussion.conversionJobId = result.job.id
                discussion.audioConversionInProgress = true
                this.discussionService.update(discussion)
            }
            comment.conversionJobId = result.job.id
            comment.audioConversionInProgress = true
            this.commentService.save(comment)
        }
    }
}