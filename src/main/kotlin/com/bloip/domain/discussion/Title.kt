package com.bloip.domain.discussion

import com.bloip.domain.ValueObject
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size


/**
 * Created by Usman Mutawakil on 9/28/22.
 */

@Embeddable
class Title : ValueObject {
    @Column(name = "title")
    @NotNull
    @Size(min = 1, max = 40, message = "Title must be between 1 and 40 characters")
    val value: String

    constructor(value: String) {
        this.value = securityFilter(value)
        validate()
    }

    override fun toString(): String {
        return this.value
    }
}