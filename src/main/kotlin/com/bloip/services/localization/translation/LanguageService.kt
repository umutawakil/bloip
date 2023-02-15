package com.bloip.services.localization.translation

import com.bloip.domain.localization.Language
import com.bloip.domain.localization.LanguageDisplayName
import com.bloip.repositories.GenericRepository
import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Created by Usman Mutawakil on 11/3/22.
 */
@Service
class LanguageService (
    @Autowired private val genericRepository: GenericRepository,
        @Autowired private val loggingService: LoggingService
    )
{
    private var canonicalLanguageByCode: MutableMap<String, Language> = ConcurrentHashMap<String, Language>()
    private var languageForLanguageByCode: MutableMap<Language, MutableMap<String, LanguageDisplayName>> = ConcurrentHashMap()

    @PostConstruct
    fun init() {
        for(l: Language in getAllCanonical()) {
            canonicalLanguageByCode[l.code] = l
        }

        for(l: Language in getAllCanonical()) {
            val languageDisplayNameByCode: MutableMap<String, LanguageDisplayName> = ConcurrentHashMap()

            /** Get every language written in language l **/

            for(languageDisplayName: LanguageDisplayName in genericRepository.findAllBy("SELECT l FROM LanguageDisplayName l WHERE l.translationLanguage.id = ${l.id}", targetClass = LanguageDisplayName::class.java)) {
                languageDisplayNameByCode[languageDisplayName.canonicalLanguage.code] = languageDisplayName
            }
            languageForLanguageByCode[l] = languageDisplayNameByCode
            loggingService.log("Initialized languages: " + languageDisplayNameByCode.size + ", in ${l.canonicalName}")
        }
        loggingService.log("Languages loaded: " + canonicalLanguageByCode.keys)
    }

    fun findById(languageId: Long) : Language {
        return genericRepository.findById(id = languageId, targetClass = Language::class.java)!!
    }

    fun getCanonicalByCode(code: String) : Language? {
        return canonicalLanguageByCode[code]
    }

    fun getAllCanonical() : Iterable<Language>  {
        return this.genericRepository.findAll(Language::class.java)
    }

    fun getLanguageDisplayNameByCode(language: Language, code: String) : LanguageDisplayName {
        return languageForLanguageByCode[language]!![code]!!
    }

    fun getAllLanguageDisplayNames(language: Language) : Iterable<LanguageDisplayName>  {
        return languageForLanguageByCode[language]!!.values.sortedBy { it.name }
    }

    fun save(languageDisplayName: LanguageDisplayName) : LanguageDisplayName {
        return genericRepository.save(languageDisplayName)
    }
}