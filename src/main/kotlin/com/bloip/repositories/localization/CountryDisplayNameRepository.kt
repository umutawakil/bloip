package com.bloip.repositories.localization

import com.bloip.domain.localization.CountryDisplayName
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/4/22.
 */
interface CountryDisplayNameRepository : CrudRepository<CountryDisplayName, Long>{
    //@Query(value = "FROM CountryDisplayName c WHERE c.language.id = ?1")
    fun findByLanguageId(languageId: Long) : List<CountryDisplayName>
}