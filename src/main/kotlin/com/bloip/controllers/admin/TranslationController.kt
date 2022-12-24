package com.bloip.controllers.admin

import com.bloip.domain.localization.TranslationKey
import com.bloip.services.LoggingService
import com.bloip.services.localization.translation.LanguageService
import com.bloip.services.localization.translation.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.annotation.Secured
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import javax.transaction.Transactional

/**
 * TODO: Manual version of this has never been tested and the automatic part seems to not work from time to time do to
 * some sort of random redirect issue and or browser caching. Simply retrying solves the problem. Nos precisamos um melhor jeito
 * a fazer isto!!!
 */
@Transactional
@Controller
@Secured("ROLE_SHOGUN")
class TranslationController(
    @Autowired val translationService: TranslationService,
    @Autowired val languageService: LanguageService,
    @Autowired val loggingService: LoggingService
) {

    @GetMapping("/castelo/tower-of-babel")
    fun index(
            model: Model,
            @RequestParam(required = false) siteTranslationContextId: Long?,
            @RequestParam(required = false) languageId: Long?,
            @RequestParam(required = false) translationKeyId: Long?,
            @RequestParam(required = false) updated:Boolean?

        ): String {

        model["siteTranslationContexts"]  = translationService.getAllSiteTranslationContexts()
        model["languages"]                = languageService.getAllCanonical()

        if(updated != null && updated == true) {
            model["updated"] = true
        }

        if(siteTranslationContextId != null) {
            model["siteTranslationContextId"] = siteTranslationContextId
            model["translationKeys"]          = translationService.getKeysForContext(siteTranslationContextId = siteTranslationContextId)

            if(languageId != null) {
                model["languageId"]   = languageId
                model["translations"] = translationService.getTranslationsByContextAndLanguage(
                    siteTranslationContextId = siteTranslationContextId,
                    languageId = languageId
                )

                model["missingTranslations"] = translationService.missingTranslations(
                    siteTranslationContextId = siteTranslationContextId,
                    languageId = languageId
                )

                if(translationKeyId != null) {
                    model["translationKeyId"] = translationKeyId
                }
            }
        }

        return "admin/translation/index"
    }

    @PostMapping("/castelo/tower-of-babel")
    fun submitManualEntry(model: Model,
               @RequestParam(required = true) siteTranslationContextId: Long,
               @RequestParam(required = true) languageId: Long,
               @RequestParam(required = true) translationKeyId: Long,
               @RequestParam(required = true) translationKeyValue: String,
               @RequestParam(required = false) automate: Boolean?
    ): String {
        loggingService.log("Creating/Updating translation")
        loggingService.log(
            "contextId: $siteTranslationContextId, languageId: $languageId," +
                    " translationKeyId: $translationKeyId," +
                    " translationKeyValue: $translationKeyValue, automate: $automate"
        )

        if (automate != null && automate) {
            loggingService.log("Automatically generating translations for single key")
            translationService.translateForAllLanguages(
                translationKeyId = translationKeyId,
                translationKeyValue = translationKeyValue
            )
        } else {
            loggingService.log("Only updating one entry in the given languageId: $languageId")
            translationService.saveOrUpdate(
                translationKeyId    = translationKeyId,
                languageId          = languageId,
                translationKeyValue = translationKeyValue
            )
        }

        return "redirect:/castelo/tower-of-babel?siteTranslationContextId=$siteTranslationContextId&" +
                "languageId=$languageId&translationKeyId=$translationKeyId&updated=true"
    }

    @PostMapping("/castelo/tower-of-babel/new-key")
    @Transactional
    fun newKey(model: Model,
                          @RequestParam(required = true) siteTranslationContextId: Long,
                          @RequestParam(required = true) languageId: Long,
                          @RequestParam(required = true) translationKeyName: String,
                          @RequestParam(required = true) translationKeyValue: String
    ): String {
        loggingService.log("Creating/Updating translation")
        loggingService.log(
            "contextId: $siteTranslationContextId, languageId: $languageId," +
                    " translationKeyName: $translationKeyName," +
                    " translationKeyValue: $translationKeyValue "
        )

        loggingService.log("Automatically generating translations for single new key")
        val translationKey: TranslationKey = translationService.createNewKey(
            keyName = translationKeyName,
            siteTranslationContextId = siteTranslationContextId
        )

        translationService.translateForAllLanguages(
            translationKeyId = translationKey.id,
            translationKeyValue = translationKeyValue
        )

        return "redirect:/castelo/tower-of-babel?siteTranslationContextId=$siteTranslationContextId&" +
                "languageId=$languageId&translationKeyId=${translationKey.id}&updated=true"
    }

    /** TODO: The code below works but is very risky to run once the database is fully populated, which it is. Don't reenable this
     * till its been tested against a stocked db and verify it hasnt screwed up any relations. Foreign Keys and cascades on delete are in place but just in case.
     */
    /*@PostMapping("/auto-keys")
    fun submitRequestForAutoTranslateForKeys(
        @RequestParam(required = true) languageId: Long
    ): String {
        loggingService.log("Importing translations for keys in languageId: $languageId...")

        translationService.autoTranslateKeysForLanguage(languageId = languageId)

        return "redirect:/tower-of-babel?languageId=$languageId&updated=true"
    }


    @PostMapping("/auto-countries")
    fun submitRequestForAutoTranslateForCountries(
        @RequestParam(required = true) languageId: Long
    ): String {
        loggingService.log("Importing translations for countries in languageId: $languageId...")

        translationService.autoTranslateCountriesForLanguage(languageId = languageId)

        return "redirect:/tower-of-babel?languageId=$languageId&updated=true"
    }

    @PostMapping("/auto-languages")
    fun submitRequestForAutoTranslateForLanguages(
        @RequestParam(required = true) languageId: Long
    ): String {
        loggingService.log("Importing translations for languages in languageId: $languageId...")

        translationService.autoTranslateLanguagesForLanguage(languageId = languageId)

        return "redirect:/tower-of-babel?languageId=$languageId&updated=true"
    }*/
}