package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/1/22.
 */
@Entity
@Table(name = "country")
class Country : StandardDomainObject {
    @Column(name="canonical_name")
    val canonicalName: String

    @Column
    val code: String

    constructor(canonicalName: String, code: String) {
        this.canonicalName = canonicalName
        this.code          = code
    }
}