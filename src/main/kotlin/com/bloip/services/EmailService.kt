package com.bloip.services

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsyncClientBuilder
import com.amazonaws.services.simpleemail.model.*
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.EmailAddress
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/25/22.
 */
@Service
class EmailService(
    @Autowired val applicationProperties: ApplicationProperties
) {

    private lateinit var sesClient: AmazonSimpleEmailServiceAsync

    //TODO: Needs translation keys

    @PostConstruct
    fun init() {
        sesClient = AmazonSimpleEmailServiceAsyncClientBuilder.standard().
        withCredentials(
            AWSStaticCredentialsProvider(
            BasicAWSCredentials(
                applicationProperties.awsUploadAccessKey, applicationProperties.awsUploadSecretKey
            )
        )
        ).build()
    }

    fun sendAccountConfirmationToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/complete-signup?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendPasswordResetToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/bloip-reset-my-password?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Reset my password",
            body      = "Click this link to reset your password <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    fun sendEmailResetToken(token: String, toAddress: EmailAddress) {
        var tokenUrl: String = applicationProperties.baseUrl + "/bloip-reset-my-email?t=$token"
        send(
            toAddress = toAddress,
            subject   = "Confirm email address",
            body      = "Click this link to confirm your email address <a href=\"$tokenUrl\"> Click here </a>"
        )
    }

    private fun send(toAddress: EmailAddress, subject: String, body: String) {
        sesClient.sendEmail(
            SendEmailRequest(
                "Bloip <${applicationProperties.serviceEmailAddress}>",
                Destination(listOf(toAddress.value)),
                Message(
                    Content(subject).withCharset(Charsets.UTF_8.name()),
                    Body().withHtml(Content(body).withCharset(Charsets.UTF_8.name()))
                )
            )
        )
    }
}