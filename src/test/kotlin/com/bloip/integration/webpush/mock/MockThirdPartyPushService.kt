package com.bloip.integration.webpush.mock

import com.bloip.domain.webpush.WebPushSubscription
import com.bloip.jobs.webpush.ThirdPartyWebPushService
/**
 * Created by Usman Mutawakil on 9/21/22.
 */
class MockThirdPartyPushService : ThirdPartyWebPushService {
    var sent: Boolean = false
    var callCount     = 0

    constructor() : super() {
        sent      = false
        callCount = 0
    }

    override fun send(webPushSubscription: WebPushSubscription) {
        sent = true
        callCount++
    }
}