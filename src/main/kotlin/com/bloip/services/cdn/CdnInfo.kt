package com.bloip.services.cdn

/**
 * Created by Usman Mutawakil on 9/27/22.
 */
class CdnInfo(
        val uuid: String,
        val policy: String,
        val signature: String,
        val fileName: String,
        val audioCdnUploadUrl: String,
        val date: String,
        val credential: String,
        val redirectUrl: String,
        val censured: Boolean
    )