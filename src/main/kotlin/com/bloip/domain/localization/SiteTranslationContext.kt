package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
@Entity
@Table(name = "site_translation_context")
class SiteTranslationContext: StandardDomainObject {
    @Column
    val context: String

    constructor(context: String) {
        this.context = context
    }
}