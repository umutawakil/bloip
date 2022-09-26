package com.bloip.jobs.webpush

import com.bloip.domain.webpush.WebPushSubscription
import com.bloip.jobs.webpush.client.WebPushClient
import com.bloip.services.LoggingService

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.net.http.HttpResponse
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 9/22/22.
 */
@Service
class ThirdPartyWebPushService {
    @Autowired var loggingService: LoggingService? = null
    val webPushClient: WebPushClient = WebPushClient()
    var serverPublicKey: String?  = null
    var serverPrivateKey: String? = null

    @PostConstruct
    fun init() {
        serverPublicKey  = System.getenv("VAPID_PUBLIC_KEY")
        serverPrivateKey = System.getenv("VAPID_PRIVATE_KEY")

        //If in test mode we don't want any cryptography logic exposed since the private key will be empty and cause problems
        if (serverPrivateKey == null) {
            println("WARNING: VAPID private key not detected. Not fully initializing third party push service. Probably running in integration test")
        }
    }

    fun send(webPushSubscription: WebPushSubscription) {
        val response: HttpResponse<String> = webPushClient.push(
            publicKey            = serverPublicKey!!,
            privateKey           = serverPrivateKey!!,
            subscriptionEndpoint = webPushSubscription.endpoint
        )

        loggingService!!.log("HTTP Response: ${response.statusCode()}")
        loggingService!!.log("Response: ${response.body()}")

        if (!(response.statusCode() == 201 || response.statusCode() == 200)) {
            RuntimeException(response.body()).printStackTrace()
        }
    }
}