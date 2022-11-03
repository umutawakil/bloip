package com.bloip.services

import com.bloip.domain.Country
import com.bloip.repositories.CountryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/1/22.
 */
@Service
class CountryService(
    @Autowired private val countryRepository: CountryRepository,
    @Autowired private val loggingService: LoggingService
)
{
    private var countryByCode: MutableMap<String, Country> = ConcurrentHashMap<String, Country>()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing country service...")
        for(country:Country in countryRepository.findAll().sortedBy { it.name }) {
            countryByCode[country.code] = country
        }
        loggingService.log("Initialized countries: " + countryByCode.values.size)
    }

    fun getByCode(code: String) : Country? {
        return countryByCode[code]
    }

    fun getAll() : Collection<Country> {
        return countryByCode.values
    }
}