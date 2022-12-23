package com.bloip.services.audioconversion

import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.discussion.Discussion
import com.bloip.msc.Constants
import com.bloip.services.LoggingService
import com.bloip.services.audioconversion.workers.ConversionRequestProducer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * This class starts the file conversion process by pushing the commentId of the associated recording onto an SQS stack
 * using a bounded executionService so that in the event there is a backlog of conversion requests trying to get into the SQS queue,
 * they can pile up locally on the executorService's local queue.
 *
 * Concurrently there are two other consumer classes running independent of this one. They live in the workers package.
 * One reads from the queue this one pushes to and the other reads from a success queue triggered by AWS lambda when a mediaconvert job has completed.
 * The consumer that reads the media conversion requests also calls the AWSMediaConvert producer because it will use the local Bloip specific domain
 * data in the conversion request pulled off the queue to build an aws specific dto and send that to the AWS managed Mediaconvert queue through the AWS Mediaconver
 * sdk. When its completed conversion (AWS mediaconvert) it's success event will cause a lambda function to push onto the ConversionCompleteQueue.
 * The consumer of that queue will update flags on the comment object and discussion object if relevant
 * (such as a comment on track 0 which is the discussion's track). Each message processed by a consumeer is deleted after it is pulled off the queue.
 * In the future it will be nice to have some retry logic and deadletter queues but for now I'm treating bad records as low cost and more than likely
 * a background job will build up or "Re-Enqueue" comments detected to be pending/needs conversion.
 */
@Service
class MediaConversionService (
    @Autowired private val applicationProperties: ApplicationProperties,
    @Autowired private val loggingService: LoggingService,
    @Autowired private val conversionRequestProducer: ConversionRequestProducer,
) : AudioConversionRequestService {
    @PostConstruct
    fun init() {}

    override fun startConvertingAudioFile(discussion: Discussion, trackNumber: Int) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            loggingService.log("MediaConversion.startConvertingAudioFile skipped due to remote services being disabled.")
            return
        }
        conversionRequestProducer.startConvertingAudioFile(discussion = discussion, trackNumber = trackNumber)
    }
}