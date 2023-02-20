package com.bloip.services.audioconversion.workers

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder
import com.amazonaws.services.sqs.model.Message
import com.amazonaws.services.sqs.model.ReceiveMessageRequest
import com.amazonaws.services.sqs.model.ReceiveMessageResult
import com.bloip.services.admin.AdminService
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
            adminService: AdminService
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
                    adminService.recordException("Exception while processing message body: ${m.body}", exception)
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

        fun runInLoop(threadName: String,adminService: AdminService,  threadSleepMillis: Long, work: ()-> Unit) {
            val thread = Thread {
                var lastTime: Long
                while (true) {
                    lastTime = System.currentTimeMillis()
                    try {
                        work()
                    } catch (exception: Exception) {
                        adminService.recordException(exception)
                    }
                    var elapsed = System.currentTimeMillis() - lastTime
                    if (elapsed < threadSleepMillis) {
                        val diff = threadSleepMillis - elapsed
                        Thread.sleep(diff)
                    }
                }
            }
            thread.name = threadName
            thread.start()
        }
    }
}