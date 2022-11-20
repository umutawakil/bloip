package com.bloip.repositories.localization

import com.bloip.domain.localization.Language
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/2/22.
 */
interface LanguageRepository: CrudRepository<Language, Long> {
}