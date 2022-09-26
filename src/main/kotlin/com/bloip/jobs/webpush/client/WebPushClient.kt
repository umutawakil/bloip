package com.bloip.jobs.webpush.client

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.bouncycastle.jce.provider.BouncyCastleProvider

import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.Security
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey

import java.util.*


/**
 * Created by Usman Mutawakil on 9/23/22.
 */
class WebPushClient {
    val DAY_IN_MILLIS: Long = 86400 * 10000 // 24 hours in milliseconds
    val MONTH_IN_SECS: Long = 3600*24 * 30

    companion object {
        init {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
                println("Crypto Provider missing and set to: " + Security.getProvider(BouncyCastleProvider.PROVIDER_NAME).name)
            } else {
                println("Crypto provider found:  "  + Security.getProvider(BouncyCastleProvider.PROVIDER_NAME).name)
            }
        }
    }
    fun push(publicKey: String,
             privateKey: String,
             subscriptionEndpoint: String
    ) : HttpResponse<String> {

        var token = createToken(publicKey, privateKey, subscriptionEndpoint)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(subscriptionEndpoint))
            //.POST(HttpRequest.BodyPublishers.noBody())
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .header("Authorization", "vapid  t=$token, k=$publicKey")
            .header("Crypto-Key", "p256ecdsa=$publicKey")
            .header("Content-Encoding", "aes128gcm")
            .header("TTL", "${MONTH_IN_SECS}")
            .build()

        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun createToken(publicKey: String, privateKey: String, subscriptionEndpoint: String) : String {
        val url = URL(subscriptionEndpoint)
        val algorithm       = Algorithm.ECDSA256(
            SecurityUtil.loadPublicKey(publicKey) as ECPublicKey,
            SecurityUtil.loadPrivateKey(privateKey) as ECPrivateKey
        )

        return JWT.create()
            .withAudience("${url.protocol}://${url.host}")
            .withSubject("mailto:admin@bloip.com")
            .withExpiresAt(Date(System.currentTimeMillis() + (1000*60*60*12)))
            .sign(algorithm)
    }
}