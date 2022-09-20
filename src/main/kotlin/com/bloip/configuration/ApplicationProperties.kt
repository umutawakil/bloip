package com.bloip.configuration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Created by Usman Mutawakil on 6/22/22.
 */
@ConfigurationProperties("app")
@ConstructorBinding
class ApplicationProperties(
    var discussionsPerPage: Int,
    var audioBucketURL: String,
    var baseURL: String,
    var inboxItemsPerPage: Int,
    var commentsPerPage: Int,
    var maxTitleLength: Int,
    var applicationServerKey: String
)