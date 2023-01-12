package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 11/2/22.
 */

@Entity
@Table(name="language")
class Language : StandardDomainObject
{
    companion object {
        val ENGLISH_ID = 1L
    }

    @Column(name="canonical_name")
    val canonicalName: String

    @Column
    val code: String

    constructor(canonicalName: String, code: String) {
        this.canonicalName = canonicalName
        this.code          = code
    }
}