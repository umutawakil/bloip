package com.bloip.services.localization.translation

import com.bloip.domain.localization.*
import com.bloip.repositories.localization.*
import com.bloip.services.LoggingService
import com.bloip.services.localization.CountryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import javax.annotation.PostConstruct
import javax.transaction.Transactional

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
@Service
class TranslationService(
    @Autowired private val translationKeyRepository: TranslationKeyRepository,
    @Autowired private val translationRepository: TranslationRepository,
    @Autowired private val siteTranslationContextRepository: SiteTranslationContextRepository,
    @Autowired private val externalTranslationService: ExternalTranslationService,
    @Autowired private val countryService: CountryService,
    @Autowired private val countryDisplayNameRepository: CountryDisplayNameRepository,
    @Autowired private val languageService: LanguageService,
    @Autowired private val loggingService: LoggingService
)
{
    private val translationMap: MutableMap<String, MutableMap<Language, MutableMap<String, String>>> = HashMap()

    @PostConstruct
    fun init() {
        for (context: SiteTranslationContext in siteTranslationContextRepository.findAll()) {
            val languageMap: MutableMap<Language, MutableMap<String, String>> = HashMap()
            for(l in languageService.getAllCanonical()) {
                val translationKeys: List<TranslationKey> = translationKeyRepository.findBySiteTranslationContextId(
                    id = context.id
                )

                val keyMap: MutableMap<String, String> = HashMap()
                for (k in translationKeys) {
                    //loggingService.log("TranslationKeyId: ${k.id}, key: ${k.key}, language: ${l.canonicalName}")
                    val translation: Translation? = CollectionUtils.firstElement(translationRepository.findByKeyIdAndLanguageId(
                        keyId = k.id,
                        languageId = l.id
                    ))
                    if (translation != null) {
                        keyMap[k.key] = translation.value
                    } else {
                        //TODO: This should happen when only the english translation exists. That can happen if the translation is made in the DB
                        loggingService.log("Key with no translation found. Key: ${k.id}, language: ${l.canonicalName}")
                    }
                }
                languageMap[l] = keyMap
            }
            translationMap[context.context] = languageMap
        }
    }

    fun getTranslationMap(context: String, language: Language) : Map<String, String> {
        return translationMap[context]!![language]!!
    }

    fun getAllSiteTranslationContexts() : Iterable<SiteTranslationContext> {
        return this.siteTranslationContextRepository.findAll()
    }

    fun getKeysForContext(siteTranslationContextId: Long) : List<TranslationKey> {
        val siteTranslationContext: SiteTranslationContext = siteTranslationContextRepository.findById(siteTranslationContextId).get()

        return translationKeyRepository.findAll().filter { it.siteTranslationContext == siteTranslationContext}
    }

    fun getTranslationsByContextAndLanguage(siteTranslationContextId: Long, languageId: Long) : List<Translation> {
        val siteTranslationContext: SiteTranslationContext = siteTranslationContextRepository.findById(siteTranslationContextId).get()
        val language: Language = languageService.findById(languageId)

        val keyIdsForContext: Set<Long> = translationKeyRepository.findAll().filter { it.siteTranslationContext == siteTranslationContext }.map {it.id}.toSet()

        return translationRepository.findAll().filter {
            keyIdsForContext.contains(it.key.id) && (it.language == language)
        }
    }

    fun createNewKey(keyName: String, siteTranslationContextId: Long) : TranslationKey {
        return translationKeyRepository.save(
            TranslationKey(
                key = keyName,
                siteTranslationContext = siteTranslationContextRepository.findById(siteTranslationContextId).get()
            )
        )
    }

    fun saveOrUpdate(translationKeyId: Long, languageId: Long, translationKeyValue: String) : Translation {
        val result: List<Translation> = translationRepository.findByKeyIdAndLanguageId(keyId = translationKeyId, languageId = languageId)
        val existingTranslation: Translation? = if (result.isEmpty()) null else {result[0]}

        if (existingTranslation != null) {
            //Update the existing
            loggingService.log("Updating existing translation")
            existingTranslation.value = translationKeyValue
            return translationRepository.save(existingTranslation)
        }

        loggingService.log("Creating brand new translation")
        return translationRepository.save(
            Translation(
                key      = translationKeyRepository.findById(translationKeyId).get(),
                language = languageService.findById(languageId),
                value    = translationKeyValue
            )
        )
    }

    fun missingTranslations(siteTranslationContextId: Long, languageId: Long) : Int {
        val total      = getKeysForContext(siteTranslationContextId = siteTranslationContextId).size
        val translated = getTranslationsByContextAndLanguage(
            siteTranslationContextId = siteTranslationContextId,
            languageId = languageId
        ).size
        println("Total: $total, translated: $translated, diff=${total - translated}")

        return total - translated
    }

    fun autoTranslateKeysForLanguage(languageId: Long) {
        val targetLanguage: Language               = languageService.findById(languageId)
        val englishTranslations: List<Translation> = translationRepository.findByLanguageId(Language.ENGLISH_ID)

        val newTranslations: List<Translation> = englishTranslations.map {
            Translation(
                key = it.key,
                language = targetLanguage,
                value = externalTranslationService.translate(input = it.value, targetLanguage)
            )
        }

        newTranslations.forEach {
            translationRepository.save(it)
        }
    }

    /** Populate the CountryDisplayName object/table **/
    fun autoTranslateCountriesForLanguage(languageId: Long) {
        val targetLanguage: Language     = languageService.findById(languageId)
        val countries: Iterable<Country> = countryService.getAllCanonicalCountries()

        val newTranslations: List<CountryDisplayName> = countries.map {
            CountryDisplayName(
                name = externalTranslationService.translate(input = it.canonicalName, targetLanguage = targetLanguage),
                language = targetLanguage,
                country = it
            )
        }

        newTranslations.forEach {
            countryDisplayNameRepository.save(it)
        }
    }

    /** Populate the LanguageDisplayName object/table **/
    fun autoTranslateLanguagesForLanguage(languageId: Long) {
        val targetLanguage: Language      = languageService.findById(languageId)
        val languages: Iterable<Language> = languageService.getAllCanonical()

        val newTranslations: List<LanguageDisplayName> = languages.map {
            LanguageDisplayName(
                name = externalTranslationService.translate(input = it.canonicalName, targetLanguage = targetLanguage),
                canonicalLanguage = it,
                translationLanguage = targetLanguage
            )
        }

        newTranslations.forEach {
            languageService.save(it)
        }
    }

    @Transactional
    fun translateForAllLanguages(translationKeyId: Long, translationKeyValue: String) {
        val result: Translation = saveOrUpdate(
            translationKeyId    = translationKeyId,
            languageId          = Language.ENGLISH_ID,
            translationKeyValue = translationKeyValue
        )
        translateForAllLanguages(englishTranslation = result)
    }

    private fun translateForAllLanguages(englishTranslation: Translation) {
        val languages: Iterable<Language> = languageService.getAllCanonical()

        for (language:Language in languages) {
            if (language.id == Language.ENGLISH_ID || language.code == "en") {
                continue
            }

            saveOrUpdate(
                translationKeyId    = englishTranslation.key.id,
                languageId          = language.id,
                translationKeyValue = externalTranslationService.translate(
                    input           = englishTranslation.value,
                    targetLanguage  = language
                )
            )
        }
    }
}