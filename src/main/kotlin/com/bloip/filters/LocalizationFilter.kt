package com.bloip.filters

import com.bloip.domain.localization.CountryDisplayName
import com.bloip.domain.localization.Language
import com.bloip.services.LoggingService
import com.bloip.services.localization.CountryService
import com.bloip.services.localization.translation.LanguageService
import com.bloip.services.localization.translation.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

/**
 * Created by Usman Mutawakil on 11/5/22.
 */
@Component
class LocalizationFilter(
    @Autowired val languageService: LanguageService,
    @Autowired val translationService: TranslationService,
     @Autowired val loggingService: LoggingService,
    @Autowired val countryService: CountryService
  ) : Filter
{
    override fun doFilter(request: ServletRequest, response: ServletResponse?, chain: FilterChain) {
        val req: HttpServletRequest  = request as HttpServletRequest
        val httpSession: HttpSession = getSession(request)
        val language: Language       = getAndSetLanguage(req, httpSession)

        getAndSetTranslations(prefix = "header",httpSession = httpSession, language = language)
        getAndSetTranslations(prefix = "footer",httpSession = httpSession, language = language)
        getAndSetCountryDisplayName(httpSession = httpSession, language = language, countryCode = getCountryCode(req))

        chain.doFilter(request, response);
    }

    fun getAndSetTranslations(prefix: String, httpSession: HttpSession, language: Language) : Map<String, String> {
        val newTranslations: Map<String, String> = translationService.getTranslationMap(context = prefix,language)
        httpSession.setAttribute("${prefix}Translations", newTranslations)
        return newTranslations
    }

    //TODO: How should the browsers locals be used against cloudfront?
    fun getCountryCode(req: HttpServletRequest) : String {
        val cloudFrontViewCountry: String? = req.getHeader("CloudFront-Viewer-Country")
        loggingService.log("Cloudfront Country: $cloudFrontViewCountry")
        if(cloudFrontViewCountry?.isNotEmpty() == true) {
            loggingService.log("Cloudfront Country: $cloudFrontViewCountry")
            return cloudFrontViewCountry
        }

        for(locale: Locale in req.locales) {
            if(locale.country.isNotEmpty()) {
                println("Country From Locale: " + locale.country)
                return locale.country
            }
        }
        return "us"
    }

    fun getAndSetCountryDisplayName(httpSession: HttpSession, language: Language, countryCode: String) : CountryDisplayName? {
        val countryDisplayName: CountryDisplayName = countryService.getCountryDisplayName(language = language, code = countryCode)!!
        httpSession.setAttribute("countryDisplayName", countryDisplayName)
        return countryDisplayName
    }

    fun getAndSetLanguage(request: HttpServletRequest, httpSession: HttpSession) : Language {
        /** Check first for a user selected language **/
        val userChosenCode: String? = request.getParameter("l")
        if(userChosenCode != null) {
            var language =  languageService.getCanonicalByCode(userChosenCode)!!

            httpSession.setAttribute("language", language)
            return language
        }

        /** If language is already set than use that **/
        var language: Language? = httpSession.getAttribute("language") as Language?
        if(language != null) {
            return language
        }

        /** See if any of the users set languages are supported **/
        val locals: List<Locale> = request.locales.toList()
        for(l in locals) {
            val canonicalLanguage: Language? = languageService.getCanonicalByCode(code = l.language)
            if(canonicalLanguage != null) {
                httpSession.setAttribute("language", canonicalLanguage)
                return canonicalLanguage
            }
        }

        /** Default is english **/
        var defaultLanguage =  languageService.getCanonicalByCode(code = "en")!!
        httpSession.setAttribute("language", defaultLanguage)
        return defaultLanguage
    }

    fun getSession(req: HttpServletRequest): HttpSession {
        return req.getSession(false) ?: req.getSession(true)
    }
}