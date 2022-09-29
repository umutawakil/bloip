package com.bloip.services.cdn

import org.apache.commons.codec.digest.HmacUtils
import org.json.JSONObject
import software.amazon.awssdk.utils.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


/**
 * Created by Usman Mutawakil on 9/26/22.
 */
class S3Uploader {
    companion object {
        const val ALGORITHM      = "HmacSHA256"
        const val FILE_EXTENSION = ".mp3"
    }
    private val awsSecretKey: String
    private val awsAccessKey: String
    private val bucketName: String
    private val region: String
    private val policyDurationMillis: Long
    private val redirectURL: String

    constructor(bucketName: String, region: String, awsSecretKey: String, awsAccessKey: String, policyDurationHours: Int, redirectUrl: String) {
        this.bucketName           = bucketName
        this.region               = region
        this.awsSecretKey         = awsSecretKey
        this.awsAccessKey         = awsAccessKey
        this.policyDurationMillis = policyDurationHours * 3600L * 1000L
        this.redirectURL          = redirectUrl
    }

    fun generateFormValue(userId: Long, audioCdnUploadUrl: String) : CdnInfo {
        val uuid: String           = UUID.randomUUID().toString()
        val instantTine: Instant   = Instant.now()
        val expirationDate: String = DateUtils.formatIso8601Date(instantTine.plusMillis(this.policyDurationMillis))
        val xmzDate: String        = calculateTimeStamp(seedTime = instantTine.toEpochMilli())
        val numericalDate          = calculateNumericalDate(seedTime = instantTine.toEpochMilli())
        val fileName               = "$userId-$uuid$FILE_EXTENSION"
        val credential             = "${this.awsAccessKey}/${numericalDate}/${this.region}/s3/aws4_request"

        val policy = generatePolicy(
            fileName            = fileName,
            expirationTimestamp = expirationDate,
            xmzDate             = xmzDate,
            credential          = credential
        )

        val signature = sign(
            input         = policy,
            numericalDate = numericalDate
        )

        return CdnInfo(
            uuid              = uuid,
            policy            = policy,
            signature         = signature,
            fileName          = fileName,
            audioCdnUploadUrl = audioCdnUploadUrl,
            date              = xmzDate,
            credential        = credential,
            redirectUrl       = this.redirectURL
        )
    }

    private fun generatePolicy(
        fileName: String,
        expirationTimestamp: String,
        xmzDate: String,
        credential: String
    ) : String {

        //TODO: Isn't she lovely?.....
        val policyTemplate =
            """{ "expiration": "$expirationTimestamp",
                  "conditions": [
                        {"bucket": "${this.bucketName}"},
                        {"key": "$fileName"},
                        {"acl": "public-read"},
                        {"success_action_redirect": "${this.redirectURL}"},
                        {"Content-Type": "audio/mpeg"},
                        {"x-amz-meta-uuid": "14365123651274"},
                        {"x-amz-server-side-encryption": "AES256"},
                        {"x-amz-credential": "$credential"},
                        {"x-amz-algorithm": "AWS4-HMAC-SHA256"},
                        {"x-amz-date": "$xmzDate"}
                  ]
               }"""

        return Base64.getEncoder().encodeToString(
            JSONObject(policyTemplate).toString().toByteArray()
        )
    }

    private fun sign(input: String, numericalDate:String): String {
        val secret: ByteArray               = ("AWS4" + this.awsSecretKey).toByteArray()
        val dateKey: ByteArray              = hmac(secret, numericalDate)
        val dateRegionKey: ByteArray        = hmac(dateKey,this.region)
        val dateRegionServiceKey: ByteArray = hmac(dateRegionKey,"s3")
        val signingKey: ByteArray           = hmac(dateRegionServiceKey, "aws4_request")

        return HmacUtils(ALGORITHM, signingKey).hmacHex(input)
    }
    private fun hmac(key:ByteArray, data: String) : ByteArray {
        return HmacUtils(ALGORITHM, key).hmac(data)
    }

    //TODO: Time will become an issue. Time to look into the nitty gritty details of GMT vs UTC, time zones, etc.
    private fun calculateTimeStamp(seedTime: Long) : String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
        df.timeZone = tz
        return df.format(Date(seedTime))
    }
    private fun calculateNumericalDate(seedTime: Long) : String {
        val tz = TimeZone.getTimeZone("UTC")
        val df: DateFormat = SimpleDateFormat("yyyyMMdd")
        df.timeZone = tz
        return df.format(Date(seedTime))
    }
}