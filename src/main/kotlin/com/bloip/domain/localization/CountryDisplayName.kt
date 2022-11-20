package com.bloip.domain.localization

import com.bloip.domain.StandardDomainObject
import javax.persistence.*

/**
 * Created by Usman Mutawakil on 11/4/22.
 */
@Entity
@Table(name = "country_display_name")
class CountryDisplayName : StandardDomainObject {
    @Column
    val name: String

    @ManyToOne(optional = false)
    @JoinColumn(name = "country_id", referencedColumnName = "id", nullable = false)
    val country: Country

    @ManyToOne(optional = false)
    @JoinColumn(name = "language_id", referencedColumnName = "id", nullable = false)
    val language: Language

    constructor(name: String, country: Country, language: Language) {
        this.name     = name
        this.country  = country
        this.language = language
    }
}