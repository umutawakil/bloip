package com.bloip.domain

import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.Email

/**
 * Created by Usman Mutawakil on 11/23/22.
 */
@Embeddable
class EmailAddress : ValueObject {
    @Email
    @Column(name="username")
    val value: String

    constructor(email: String) {
        this.value = email
        this.validate()
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return value == other
    }
}