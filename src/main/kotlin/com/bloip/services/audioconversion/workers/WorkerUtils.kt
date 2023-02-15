package com.bloip.services.audioconversion.workers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
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
            forEachMessage: (inputMessage: Message) -> Unit,
            loggingService: LoggingService
        ) {
            val result: Future<ReceiveMessageResult> = getMessages(
                queryUrl          = queryUrl,
                sqsClient         = sqsClient,
                maxQueueBatchSize = maxAudioQueueBatchSize
            )
            val receiveMessageResult: ReceiveMessageResult = result.get()

            for (m: Message in receiveMessageResult.messages) {
                try {
                    forEachMessage(m)
                    sqsClient.deleteMessageAsync(queryUrl, m.receiptHandle)

                } catch (exception: Exception) {
                    loggingService.error("Exception while processing message body: ${m.body}", exception)
                    sqsClient.deleteMessageAsync(queryUrl, m.receiptHandle)
                }
            }
        }

        private fun getMessages(queryUrl: String, sqsClient: AmazonSQSAsync, maxQueueBatchSize: Int) : Future<ReceiveMessageResult> {
            return sqsClient.receiveMessageAsync(
                ReceiveMessageRequest().
                withQueueUrl(queryUrl).
                withMaxNumberOfMessages(maxQueueBatchSize)
            )
        }

        fun runInLoop(threadName: String, loggingService: LoggingService, threadSleepMillis: Long, work: ()-> Unit) {
            val thread = Thread {
                var lastTime: Long
                while (true) {
                    lastTime = System.currentTimeMillis()
                    try {
                        //loggingService.log("Performing work for $threadName....")
                        work()
                    } catch (exception: Exception) {
                        loggingService.error(threadName, exception)
                    }
                    var elapsed = System.currentTimeMillis() - lastTime
                    if (elapsed < threadSleepMillis) {
                        val diff = threadSleepMillis - elapsed
                        loggingService.log("Waiting $diff ms in thread: $threadName")
                        Thread.sleep(diff)
                    }
                }
            }
            thread.name = threadName
            thread.start()
        }
    }
}