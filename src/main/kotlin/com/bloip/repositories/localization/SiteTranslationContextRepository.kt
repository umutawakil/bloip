package com.bloip.repositories.localization

import com.bloip.domain.localization.SiteTranslationContext
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
interface SiteTranslationContextRepository : CrudRepository<SiteTranslationContext, Long> {
}