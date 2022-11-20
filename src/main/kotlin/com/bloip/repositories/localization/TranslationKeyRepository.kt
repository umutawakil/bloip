package com.bloip.repositories.localization

import com.bloip.domain.localization.TranslationKey
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
interface TranslationKeyRepository : CrudRepository<TranslationKey, Long> {
    fun findBySiteTranslationContextId(id: Long): List<TranslationKey>
}