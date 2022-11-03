package com.bloip.domain

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 11/1/22.
 */
@Entity
@Table(name = "country")
class Country : StandardDomainObject {
    @Column
    val name: String
    @Column
    val code: String

    constructor(name: String, code: String) {
        this.name = name
        this.code = code
    }
}