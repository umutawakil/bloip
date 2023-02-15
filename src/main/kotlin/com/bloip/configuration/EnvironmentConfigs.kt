package com.bloip.configuration

/**
 * TODO: Eventually this should replace the ApplicationProperties class. Shouldn't need DI and spring bean loading
 * just for configuration properties.
 */
object EnvironmentConfigs {
    val audioCdnRootUrl: String? = System.getenv("AUDIO_CDN_ROOT_URL")
    val baseUrl: String?         = System.getenv("BASE_URL")
    val mscCdn: String?          = System.getenv("MSC_CDN")
}