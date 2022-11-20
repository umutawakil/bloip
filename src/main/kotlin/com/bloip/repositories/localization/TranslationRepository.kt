package com.bloip.repositories.localization

import com.bloip.domain.localization.Translation
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/2/22.
 */
interface TranslationRepository : CrudRepository<Translation, Long>{
    fun findByLanguageId(languageId: Long) : List<Translation>

    fun findByKeyIdAndLanguageId(keyId: Long, languageId: Long) : List<Translation>
}