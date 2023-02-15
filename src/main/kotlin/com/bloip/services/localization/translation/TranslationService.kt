package com.bloip.services.localization.translation

import com.bloip.domain.localization.*
import com.bloip.repositories.GenericRepository
import com.bloip.services.LoggingService
import com.bloip.services.localization.CountryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
@Service
class TranslationService(
    @Autowired private val genericRepository: GenericRepository,
    @Autowired private val externalTranslationService: ExternalTranslationService,
    @Autowired private val countryService: CountryService,
    @Autowired private val languageService: LanguageService,
    @Autowired private val loggingService: LoggingService
)
{
    private val translationMap: MutableMap<String, MutableMap<Language, MutableMap<String, String>>> = HashMap()

    @PostConstruct
    fun init() {
        for (context: SiteTranslationContext in genericRepository.findAll(SiteTranslationContext::class.java)) {
            val languageMap: MutableMap<Language, MutableMap<String, String>> = HashMap()
            for(l in languageService.getAllCanonical()) {
                val translationKeys: List<TranslationKey> = genericRepository.findAllBy(
                    "SELECT t FROM TranslationKey t WHERE t.siteTranslationContext.id = ${context.id}",
                    targetClass = TranslationKey::class.java)

                val keyMap: MutableMap<String, String> = HashMap()
                for (k in translationKeys) {
                    //loggingService.log("TranslationKeyId: ${k.id}, key: ${k.key}, language: ${l.canonicalName}")
                    //findByKeyIdAndLanguageId(keyId: Long, languageId: Long)
                    val translation: Translation? = CollectionUtils.firstElement(
                        genericRepository.findAllBy(
                            query   = "SELECT t FROM Translation t WHERE t.key.id = ${k.id} AND t.language.id = ${l.id}",
                        targetClass = Translation::class.java
                        )
                    )
                    /*val translation: Translation? = CollectionUtils.firstElement(translationRepository.findByKeyIdAndLanguageId(
                        keyId = k.id,
                        languageId = l.id
                    ))*/
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
        return this.genericRepository.findAll(SiteTranslationContext::class.java)
    }

    fun getKeysForContext(siteTranslationContextId: Long) : List<TranslationKey> {
        val siteTranslationContext: SiteTranslationContext = genericRepository.findById(id = siteTranslationContextId, targetClass = SiteTranslationContext::class.java)!!

        return genericRepository.findAll(TranslationKey::class.java).filter { it.siteTranslationContext == siteTranslationContext}
    }

    fun getTranslationsByContextAndLanguage(siteTranslationContextId: Long, languageId: Long) : List<Translation> {
        val siteTranslationContext: SiteTranslationContext = genericRepository.findById(id = siteTranslationContextId, targetClass = SiteTranslationContext::class.java)!!
        val language: Language = languageService.findById(languageId)

        val keyIdsForContext: Set<Long> = genericRepository.findAll(TranslationKey::class.java).filter { it.siteTranslationContext == siteTranslationContext }.map {it.id}.toSet()

        return genericRepository.findAll(Translation::class.java).filter {
            keyIdsForContext.contains(it.key.id) && (it.language == language)
        }
    }

    fun createNewTranslation(keyName: String, englishValue: String, siteTranslationContextId: Long) {
        val newEnglishTranslationKey =  genericRepository.save(
            TranslationKey(
                key = keyName,
                siteTranslationContext = genericRepository.findById(id = siteTranslationContextId, targetClass = SiteTranslationContext::class.java)!!
            )
        )
        translateEnglishValueIntoAllLanguages(
            translationKeyId = newEnglishTranslationKey.id,
            englishValue     = englishValue
        )
    }

    fun saveOrUpdate(translationKeyId: Long, languageId: Long, translationKeyValue: String) : Translation {
        val result: List<Translation> = genericRepository.findAllBy(
                query   = "SELECT t FROM Translation t WHERE t.key.id = $translationKeyId AND t.language.id = $languageId",
                targetClass = Translation::class.java
            )

        val existingTranslation: Translation? = if (result.isEmpty()) null else {result[0]}

        if (existingTranslation != null) {
            //Update the existing
            loggingService.log("Updating existing translation")
            existingTranslation.value = translationKeyValue
            return genericRepository.save(existingTranslation)
        }

        loggingService.log("Creating brand new translation")
        return genericRepository.save(
            Translation(
                key      = genericRepository.findById(id= translationKeyId, targetClass = TranslationKey::class.java)!!,
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
        val englishTranslations: List<Translation> = genericRepository.findAllBy(query="SELECT t FROM Translation t WHERE t.language.id = ${Language.ENGLISH_ID}", targetClass = Translation::class.java)

        val newTranslations: List<Translation> = englishTranslations.map {
            Translation(
                key = it.key,
                language = targetLanguage,
                value = externalTranslationService.translate(input = it.value, targetLanguage)
            )
        }

        newTranslations.forEach {
            genericRepository.save(it)
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
            genericRepository.save(it)
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

    private fun translateEnglishValueIntoAllLanguages(translationKeyId: Long, englishValue: String) {
        val result: Translation = saveOrUpdate(
            translationKeyId    = translationKeyId,
            languageId          = Language.ENGLISH_ID,
            translationKeyValue = englishValue
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
                                        input          = englishTranslation.value,
                                        targetLanguage = language
                                    )
            )
        }
    }

    fun changeExistingTranslationKeyValue(
        translationKeyId: Long,
        translationKeyValue: String,
        languageId: Long,
        translateForAllLanguages: Boolean
    ) {
        if (translateForAllLanguages) {
            loggingService.log("updating all language translations for given key Id")
            translateEnglishValueIntoAllLanguages(
                translationKeyId = translationKeyId,
                englishValue = translationKeyValue
            )
        } else {
            loggingService.log(
                "Only updating translation for the given translation key id $translationKeyId and given languageId: $languageId"
            )
            saveOrUpdate(
                translationKeyId    = translationKeyId,
                languageId          = languageId,
                translationKeyValue = translationKeyValue
            )
        }
    }
}