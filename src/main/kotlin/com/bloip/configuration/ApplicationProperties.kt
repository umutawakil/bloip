package com.bloip.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@ConfigurationProperties("app")
@ConstructorBinding
class ApplicationProperties(
    var environment: String,
    var discussionsPerPage: Int,
    var baseUrl: String,
    var selectedHbmDialect: String,
    var inboxItemsPerPage: Int,
    var awsUploadRegion: String,
    var awsUploadBucketName: String,
    var awsUploadSecretKey: String,
    var awsUploadAccessKey:String,
    var awsPolicyDurationHours: Int,
    var awsS3RedirectUrl: String,
    var audioCdnUploadUrl: String,
    var enableRemoteServices: String,
    var mediaConvertRole: String,
    var needsConversionQueueUrl: String,
    var conversionCompleteQueueUrl: String,
    var maxAudioQueueBatchSize: Int,
    var audioConversionConsumerThreadSleepMillis: Long,
    var mscCdn: String,
    var viewVersion: String,
    var maxDiscussionCreationsPerDay: Int,
    var eventQueueUrl: String,
    var eventTopic: String,
    var errorQueueUrl: String,
    var errorTopic: String,
    var eventNotificationInterval: Int,
    var shogunUsername: String,
    var shogunPassword: String,
    var serviceEmailAddress: String,
    var emailBucket: String,
    var emailQueueUrl: String,
    var emailQueuePollInterval: Long,
    var jwtKey: String,
    var firebaseKeyLocation: String
    )