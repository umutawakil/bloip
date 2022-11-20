package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/3/22.
 */

@Entity
@Table(name = "translation_key")
class TranslationKey: StandardDomainObject
{
    @Column(name="`key`")
    val key: String

    @ManyToOne(optional = false)
    @JoinColumn(name = "context_id", referencedColumnName = "id", nullable = false)
    val siteTranslationContext: SiteTranslationContext

    constructor(key: String, siteTranslationContext: SiteTranslationContext) {
        this.key = key
        this.siteTranslationContext = siteTranslationContext
    }
}