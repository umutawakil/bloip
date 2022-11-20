package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/4/22.
 */
@Entity
@Table(name="language_display_name")
class LanguageDisplayName : StandardDomainObject {
    @Column
    val name: String

    @ManyToOne(optional = true)
    @JoinColumn(name = "canonical_language_id", referencedColumnName = "id", nullable = false)
    val canonicalLanguage: Language

    @ManyToOne(optional = true)
    @JoinColumn(name = "translation_language_id", referencedColumnName = "id", nullable = false)
    val translationLanguage: Language

    constructor(name: String, canonicalLanguage: Language, translationLanguage: Language) {
        this.name                = name
        this.canonicalLanguage   = canonicalLanguage
        this.translationLanguage = translationLanguage
    }
}