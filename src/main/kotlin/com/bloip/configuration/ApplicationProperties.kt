package com.bloip.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@ConfigurationProperties("app")
@ConstructorBinding
class ApplicationProperties(
    var discussionsPerPage: Int,
    var baseUrl: String,
    var inboxItemsPerPage: Int,
    var commentsPerPage: Int,
    var maxTitleLength: Int,
    var applicationServerKey: String,
    var webPushRunnerCheckTime: Long,

    var webPushCountWindowHours: Int,
    var webPushMinimumResponseDelayMin: Int,
    var webPushDailyCountMax: Int,

    var awsUploadRegion: String,
    var awsUploadBucketName: String,
    var awsUploadSecretKey: String,
    var awsUploadAccessKey:String,
    var awsPolicyDurationHours: Int,
    var awsS3RedirectUrl: String,
    var audioCdnRootUrl: String,
    var audioCdnUploadUrl: String,
    var enableRemoteServices: String,
    var mediaConvertRole: String
    )