package com.bloip.domain.value

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
        //TODO: At the moment we escape on the client side
        /*val safe: String  = Jsoup.clean(inputString, Safelist.basic())
        val safer: String = safe.replace(Regex("<>:="),"")*/
        return inputString
    }
}