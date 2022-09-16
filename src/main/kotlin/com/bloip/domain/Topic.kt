package com.bloip.domain

import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 9/12/22.
 */
@Entity
@Table(name = "topic")
class Topic : StandardDomainObject {
    val name: String
    val description: String
    val country: String
    val language: String
    val friendlyId: String

    @Transient
    var count: Int = 0

    constructor(name: String, description: String, country: String, language: String, friendlyId: String) {
        this.name        = name
        this.description = description
        this.country     = country
        this.language    = language
        this.friendlyId  = friendlyId
    }
}