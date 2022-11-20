package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/2/22.
 */
@Entity
@Table(name="translation")
class Translation : StandardDomainObject
{
    @ManyToOne(optional = true)
    @JoinColumn(name = "key_id", referencedColumnName = "id", nullable = false)
    val key: TranslationKey

    @ManyToOne(optional = true)
    @JoinColumn(name = "language_id", referencedColumnName = "id", nullable = false)
    val language: Language

    @Column
    var value: String

    constructor(key: TranslationKey, language: Language, value: String) {
        this.key      = key
        this.value    = value
        this.language = language
    }
}