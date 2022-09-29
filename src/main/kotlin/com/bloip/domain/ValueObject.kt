package com.bloip.domain

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import javax.validation.*

/**
 * Created by Usman Mutawakil on 9/28/22.
 */
open class ValueObject {
    companion object {
        val factory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
        val validator: Validator = factory.validator
    }

    fun validate() {
        val validations: Set<ConstraintViolation<Any>> = validator.validate(this)
        if (validations.isNotEmpty()) {
            throw ConstraintViolationException(validations);
        }
    }

    fun securityFilter(inputString: String) : String {
        val safe: String  = Jsoup.clean(inputString, Safelist.basic())
        val safer: String = safe.replace(Regex("<>:="),"")
        if(safer.isBlank()) {
            throw IllegalArgumentException("Title is completely invalid")
        }
        return safer
    }
}