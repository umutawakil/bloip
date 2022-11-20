package com.bloip.repositories.localization

import com.bloip.domain.localization.LanguageDisplayName
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/4/22.
 */
interface LanguageDisplayNameRepository : CrudRepository<LanguageDisplayName, Long> {
    //@Query(value = "FROM LanguageDisplayName l WHERE l.translationLanguage.id = ?1")
    fun findByTranslationLanguageId(translationLanguageId: Long) : List<LanguageDisplayName>
}