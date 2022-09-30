package com.bloip.configuration

/**
 * Created by Usman Mutawakil on 9/29/22.
 */
object EnvironmentConfigs {
    val audioCdnRootUrl: String = System.getenv("AUDIO_CDN_ROOT_URL")
    val baseUrl: String = System.getenv("BASE_URL")
}