package com.bloip.repositories

import com.bloip.domain.Country
import org.springframework.data.repository.CrudRepository

/**
 * Created by Usman Mutawakil on 11/1/22.
 */
interface CountryRepository : CrudRepository<Country, Long> {
}