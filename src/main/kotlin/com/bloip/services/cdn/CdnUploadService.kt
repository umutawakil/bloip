package com.bloip.services.cdn

import com.bloip.configuration.ApplicationProperties
import com.bloip.controllers.BloipAdvice
import com.bloip.domain.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/26/22.
 */
@Service
class CdnUploadService(
    @Autowired private val applicationProperties: ApplicationProperties
) {
    private var s3Uploader: S3Uploader? = null

    @PostConstruct
    fun init() {
        this.s3Uploader = S3Uploader(
            bucketName          = applicationProperties.awsUploadBucketName,
            region              = applicationProperties.awsUploadRegion,
            awsSecretKey        = applicationProperties.awsUploadSecretKey,
            awsAccessKey        = applicationProperties.awsUploadAccessKey,
            policyDurationHours = applicationProperties.awsPolicyDurationHours, //TODO: Is this long enough? What timing issues can occur?
            redirectUrl         = applicationProperties.awsS3RedirectUrl
        )
    }

    fun getInfo(userId: User.UserId, audioInfo: BloipAdvice.AudioInfo) : CdnInfo {
        return s3Uploader!!.generateFormValue(
            userId            = userId,
            audioCdnUploadUrl = applicationProperties.audioCdnUploadUrl,
            audioInfo         = audioInfo
        )
    }
}