package com.bloip.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@ConfigurationProperties("app")
@ConstructorBinding
class ApplicationProperties(
    val discussionsPerPage: Int,
    val audioBucketURL: String,
    val baseURL: String
)