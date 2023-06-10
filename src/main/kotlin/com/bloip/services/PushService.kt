package com.bloip.services

import com.bloip.configuration.ApplicationProperties
import com.bloip.msc.Constants
import com.bloip.services.admin.AdminService
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import javax.annotation.PostConstruct


@Service
class PushService(
    @Autowired val applicationProperties: ApplicationProperties,
    @Autowired val loggingService: LoggingService,
    @Autowired val adminService: AdminService
)
{
    private lateinit var firebaseMessaging: FirebaseMessaging
    @PostConstruct
    fun init() {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }
        val googleCredentials = GoogleCredentials.fromStream(
            FileInputStream(File(applicationProperties.firebaseKeyLocation))
        )
        val options = FirebaseOptions.builder()
            .setCredentials(googleCredentials)
            .build()
        val firebaseApp: FirebaseApp = FirebaseApp.initializeApp(options)

        firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp)

        loggingService.log("Push service loaded")
    }

    fun send(deviceToken: String, subject: String, body: String) {
        if (applicationProperties.enableRemoteServices != Constants.REMOTE_SERVICES_ON) {
            return
        }

        try {
            val messageId: String = firebaseMessaging.send(
                Message.builder()
                    .setToken(deviceToken)
                    .putData("body", body)
                    .putData("subject", subject)
                    .build()
            )
            loggingService.log("Firebase message ID: $messageId")
        } catch(e: Exception) {
            adminService.recordException(
                message   = "Failed to send firebase push notification",
                exception =  e
            )
        }
    }
}