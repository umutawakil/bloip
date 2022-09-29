package com.bloip.services.cdn

import org.apache.commons.codec.digest.HmacUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 9/26/22.
 */
class S3UploaderTest {
    /** This is a full working hmac function **/
    fun hmacWithApacheCommons(algorithm: String?, data: String?, key: String?): String? {
        return HmacUtils(algorithm, key).hmacHex(data)
    }

    /** This test shows what your outputs should be for a given input if your hmac algo/function is correct **/
    @Test
    fun litmusTest() { //needs better name..
        val hmacMD5Value = "621dc816b3bf670212e0c261dc9bcdb6"

        val hmacMD5Algorithm = "HmacMD5"
        val data = "baeldung"
        val key = "123456"
        val result: String? = hmacWithApacheCommons(hmacMD5Algorithm, data, key)
        assertEquals(hmacMD5Value, result)
    }

    @Test
    fun canCreateValidS3FormValues() {
        val expectedSignature = "8afdbf4008c03f22c2cd3cdb72e4afbb1f6a588f3255ac628749a66d7f09699e"
        val expectedPolicy    = "eyAiZXhwaXJhdGlvbiI6ICIyMDE1LTEyLTMwVDEyOjAwOjAwLjAwMFoiLA0KICAiY29uZGl0aW9ucyI6IFsNCiAgICB7ImJ1Y2tldCI6ICJzaWd2NGV4YW1wbGVidWNrZXQifSwNCiAgICBbInN0YXJ0cy13aXRoIiwgIiRrZXkiLCAidXNlci91c2VyMS8iXSwNCiAgICB7ImFjbCI6ICJwdWJsaWMtcmVhZCJ9LA0KICAgIHsic3VjY2Vzc19hY3Rpb25fcmVkaXJlY3QiOiAiaHR0cDovL3NpZ3Y0ZXhhbXBsZWJ1Y2tldC5zMy5hbWF6b25hd3MuY29tL3N1Y2Nlc3NmdWxfdXBsb2FkLmh0bWwifSwNCiAgICBbInN0YXJ0cy13aXRoIiwgIiRDb250ZW50LVR5cGUiLCAiaW1hZ2UvIl0sDQogICAgeyJ4LWFtei1tZXRhLXV1aWQiOiAiMTQzNjUxMjM2NTEyNzQifSwNCiAgICB7IngtYW16LXNlcnZlci1zaWRlLWVuY3J5cHRpb24iOiAiQUVTMjU2In0sDQogICAgWyJzdGFydHMtd2l0aCIsICIkeC1hbXotbWV0YS10YWciLCAiIl0sDQoNCiAgICB7IngtYW16LWNyZWRlbnRpYWwiOiAiQUtJQUlPU0ZPRE5ON0VYQU1QTEUvMjAxNTEyMjkvdXMtZWFzdC0xL3MzL2F3czRfcmVxdWVzdCJ9LA0KICAgIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwNCiAgICB7IngtYW16LWRhdGUiOiAiMjAxNTEyMjlUMDAwMDAwWiIgfQ0KICBdDQp9"

        val s3Uploader = S3Uploader(
            bucketName          = "bucketName",
            region              = "us-east-1",
            awsSecretKey        = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            awsAccessKey        = "AKIAIOSFODNN7EXAMPLE",
            policyDurationHours = 12,
            redirectUrl         = "http://sigv4examplebucket.s3.amazonaws.com/successful_upload.html"
        )

        val s3FormValues: CdnInfo = s3Uploader.generateFormValue(
            userId = 0,
            audioCdnUploadUrl = "http://bloip.com"
        )

        println("Policy: ${s3FormValues.policy}")
        println("Signature: ${s3FormValues.signature}")
        println("FileName: ${s3FormValues.fileName}")
        //assertEquals(s3FormValues.policy, expectedPolicy)
        //assertEquals(s3FormValues.signature, expectedSignature)
        //assertTrue(s3FormValues.fileName.endsWith(S3Uploader.FILE_EXTENSION))
    }
}