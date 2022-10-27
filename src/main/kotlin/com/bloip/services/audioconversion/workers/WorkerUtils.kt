package com.bloip.services.audioconversion.workers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.bloip.domain.Comment
import com.bloip.domain.discussion.Discussion
import com.bloip.services.DiscussionService
import com.bloip.services.LoggingService
import java.util.concurrent.Future

/**
 * Created by Usman Mutawakil on 10/26/22.
 */
class WorkerUtils {
    companion object {
        fun buildAmazonSQSClientBuilder(awsUploadAccessKey: String, awsUploadSecretKey: String) : AmazonSQSAsyncClientBuilder {
            return AmazonSQSAsyncClient.asyncBuilder().withCredentials(
                AWSStaticCredentialsProvider(
                    BasicAWSCredentials(
                        awsUploadAccessKey, awsUploadSecretKey
                    )
                )
            )
        }

        fun readFromQueue(
            queryUrl: String,
            sqsClient: AmazonSQSAsync,
            maxAudioQueueBatchSize: Int,
            messageToComment: (inputMessage: Message) -> Comment?,
            callback: (comment: Comment, discussion: Discussion?) -> Unit,
            discussionService: DiscussionService, loggingService: LoggingService
        ) {
            val result: Future<ReceiveMessageResult> = getMessages(
                queryUrl = queryUrl,
                sqsClient = sqsClient,
                maxAudioQueueBatchSize = maxAudioQueueBatchSize
            )
            val receiveMessageResult: ReceiveMessageResult = result.get()
            for (m: Message in receiveMessageResult.messages) {
                try {
                    val comment: Comment? = messageToComment(m)

                    if (comment != null) {

                        val discussion: Discussion? = if (comment.trackNumber == 0) {
                            discussionService.get(discussionId = comment.discussionId)
                        } else {
                            null
                        }
                        callback(comment, discussion)
                        sqsClient.deleteMessageAsync(queryUrl, m.receiptHandle)

                    } else {
                        loggingService.error("Unable to find comment from queue in message body: ${m.body}")
                        sqsClient.deleteMessageAsync(queryUrl, m.receiptHandle)
                    }
                } catch (exception: Exception) {
                    loggingService.error("Exception while processing message body: ${m.body}", exception)
                    sqsClient.deleteMessageAsync(queryUrl, m.receiptHandle)
                }
            }
        }

        private fun getMessages(queryUrl: String,sqsClient: AmazonSQSAsync, maxAudioQueueBatchSize: Int) : Future<ReceiveMessageResult> {
            return sqsClient.receiveMessageAsync(
                ReceiveMessageRequest().
                withQueueUrl(queryUrl).
                withMaxNumberOfMessages(maxAudioQueueBatchSize)
            )
        }

        fun runInLoop(threadName: String,loggingService: LoggingService, audioConversionConsumerThreadSleepMillis: Long, work: ()-> Unit) {
            Thread {
                var lastTime: Long
                while (true) {
                    lastTime = System.currentTimeMillis()
                    try {
                        loggingService.log("Performing work for $threadName....")
                        work()
                    } catch (exception: Exception) {
                        loggingService.error(threadName, exception)
                    }
                    var elapsed = System.currentTimeMillis() - lastTime
                    if (elapsed < audioConversionConsumerThreadSleepMillis) {
                        val diff = audioConversionConsumerThreadSleepMillis - elapsed
                        println("Waiting $diff ms")
                        Thread.sleep(diff)
                    }
                }
            }.start()
        }
    }


}