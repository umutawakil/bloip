package com.bloip.domain.webpush

import com.bloip.domain.StandardDomainObject
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 9/18/22.
 */
@Entity
@Table(name = "web_push_subscription")
class WebPushSubscription : StandardDomainObject {
    val userId: Long
    val privateKey: String
    val auth: String
    val endpoint: String
    val expirationTime: String?

    constructor(userId: Long, key: String, auth: String, endpoint: String, expirationTime: String?) {
        this.userId         = userId
        this.privateKey     = key
        this.auth           = auth
        this.endpoint       = endpoint
        this.expirationTime = expirationTime
    }
}