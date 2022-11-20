package com.bloip.services.localization

import com.bloip.domain.localization.Country
import com.bloip.domain.localization.CountryDisplayName
import com.bloip.domain.localization.Language
import com.bloip.repositories.localization.CountryDisplayNameRepository
import com.bloip.repositories.localization.CountryRepository
import com.bloip.services.LoggingService
import com.bloip.services.localization.translation.LanguageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Canonical countries are the country names in english and the parent for CountryDisplayName
 */
@Service
class CountryService(
    @Autowired private val countryRepository: CountryRepository,
    @Autowired private val countryDisplayNameRepository: CountryDisplayNameRepository,
    @Autowired private val languageService: LanguageService,
    @Autowired private val loggingService: LoggingService
)
{
    private var canonicalCountryByCode: MutableMap<String, Country> = ConcurrentHashMap<String, Country>()
    private var countryForLanguageByCode: MutableMap<Language, MutableMap<String, CountryDisplayName>> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        loggingService.log("Initializing country service...")
        for(country: Country in countryRepository.findAll().sortedBy { it.canonicalName }) {
            canonicalCountryByCode[country.code.lowercase()] = country
        }

        for(l: Language in languageService.getAllCanonical()) {
            val countryDisplayNameByCode: MutableMap<String, CountryDisplayName> = ConcurrentHashMap()
            for(countryDisplayName: CountryDisplayName in countryDisplayNameRepository.findByLanguageId(l.id)) {
                countryDisplayNameByCode[countryDisplayName.country.code.lowercase()] = countryDisplayName
            }
            countryForLanguageByCode[l] = countryDisplayNameByCode
            loggingService.log("Initialized countries: " + countryDisplayNameByCode.size + ", in ${l.canonicalName}")
        }
        loggingService.log("CountryService initialized")
    }

    fun getCanonicalByCode(code: String) : Country? {
        return canonicalCountryByCode[code.lowercase()]
    }

    fun getAllCanonicalCountries() : Collection<Country> {
        return canonicalCountryByCode.values
    }

    fun getCountryDisplayName(language: Language, code: String) : CountryDisplayName? {
        return countryForLanguageByCode[language]!![code.lowercase()]!!
    }

    fun getAllCountryDisplayNames(language: Language) : List<CountryDisplayName> {
        return countryForLanguageByCode[language]!!.values.sortedBy { it.name }
    }
}