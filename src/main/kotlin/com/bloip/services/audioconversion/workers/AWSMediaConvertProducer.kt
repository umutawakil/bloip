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

import com.bloip.services.LoggingService
import com.bloip.services.admin.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.util.concurrent.*
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 10/26/22.
 */
@Component
class AWSMediaConvertProducer(
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val adminService: AdminService
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
        loggingService.log("AWSMediaConvertProducer initialized. -> RemoteServices: ${applicationProperties.enableRemoteServices}")
    }

    //TODO: This is a ridiculous way to get the endpoint URL but that is all I've found so far in the SDK examples
    private fun getMediaConvertEndpointUrl(): String {
        return buildAWSMediaClientBuilder().build().describeEndpoints(DescribeEndpointsRequest()).endpoints[0].url
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

    fun sendAWSMediaConverterRequest(discussionId: Discussion.DiscussionId, trackNumber: Int, fileName: String) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        executorService.execute {
            sendAWSMediaConverterRequestHelper(discussionId, trackNumber, fileName)
        }
    }

    private fun sendAWSMediaConverterRequestHelper(discussionId: Discussion.DiscussionId, trackNumber: Int, fileName: String) {
        val result: Future<CreateJobResult> = mediaConverterClient.createJobAsync(
            CreateJobRequest()
                .withAccelerationSettings(AccelerationSettings().withMode("DISABLED"))
                .withStatusUpdateInterval(StatusUpdateInterval.SECONDS_60)
                .withPriority(0)
                .withHopDestinations()
                .withSettings(createJobSettings(
                        fileName            = fileName,
                        awsUploadBucketName = applicationProperties.awsUploadBucketName
                    )
                )
                .withRole(applicationProperties.mediaConvertRole),
            Discussion.buildMediaConvertHandler(discussionId, trackNumber)
        )
        val jobResult: CreateJobResult = result.get()
        if(!setOf(200, 201).contains(jobResult.sdkHttpMetadata.httpStatusCode)) {
            adminService.recordException(exception = Exception("Error sending media request: " + jobResult.sdkHttpMetadata.httpStatusCode))
        }
    }

    private fun createJobSettings(fileName: String, awsUploadBucketName: String): JobSettings {
        val keyName: String = fileName
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

        val output = Output()
        output.containerSettings = ContainerSettings()
        output.containerSettings.container = "MP4"
        output.setAudioDescriptions(listOf(audioDescription))
        outputGroup.setOutputs(listOf(output))

        outputGroup.outputGroupSettings = OutputGroupSettings()
        outputGroup.outputGroupSettings.type = "FILE_GROUP_SETTINGS"
        outputGroup.outputGroupSettings.fileGroupSettings = FileGroupSettings()
        outputGroup.outputGroupSettings.fileGroupSettings.destination =
            "s3://${awsUploadBucketName}/output/"
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
        audioSelector.externalAudioFileInput = "s3://${awsUploadBucketName}/$keyName"
        //audioSelector.audioDurationCorrection    = "AUTO"
        input.audioSelectors = HashMap<String, AudioSelector>()
        input.audioSelectors["Audio Selector 1"] = audioSelector
        input.timecodeSource = "ZEROBASED"

        jobSettings.setInputs(listOf(input))

        return jobSettings
    }
}