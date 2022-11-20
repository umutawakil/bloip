package com.bloip.services.localization.translation

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.translate.AmazonTranslate
import com.amazonaws.services.translate.AmazonTranslateClient
import com.amazonaws.services.translate.model.TranslateTextRequest
import com.bloip.configuration.ApplicationProperties
import com.bloip.domain.localization.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/4/22.
 */
@Service
class ExternalTranslationService(
    @Autowired private val applicationProperties: ApplicationProperties
)
{
    private lateinit var client: AmazonTranslate

    @PostConstruct
    fun init() {
        client = buildClientBuilder(
            awsAccessKey = applicationProperties.awsUploadAccessKey,
            awsSecretKey = applicationProperties.awsUploadSecretKey
        )
    }

    private fun buildClientBuilder(awsAccessKey: String, awsSecretKey: String) : AmazonTranslate {
        return AmazonTranslateClient.builder().withCredentials(
            AWSStaticCredentialsProvider(
                BasicAWSCredentials(
                    awsAccessKey, awsSecretKey
                )
            )
        ).build()
    }

    fun translate(input: String, targetLanguage: Language) : String {
        val request = TranslateTextRequest()
            .withText(input)
            .withSourceLanguageCode("en")
            .withTargetLanguageCode(targetLanguage.code)

        return  client.translateText(request).translatedText

    }
}