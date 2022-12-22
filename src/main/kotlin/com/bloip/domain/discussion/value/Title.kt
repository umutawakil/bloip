package com.bloip.domain.discussion.value

import com.bloip.domain.value.ValueObject
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.NotEmpty


/**
 * Created by Usman Mutawakil on 9/28/22.
 */

@Embeddable
class Title : ValueObject {

    companion object {
        const val MAX_TITLE_LENGTH=60
    }

    @Column(name = "title")
    @NotEmpty
    val value: String

    constructor(value: String) {
        this.value = securityFilter(value)
        validate()
        if (this.value.isEmpty() || (this.value.length > MAX_TITLE_LENGTH)) {
            throw IllegalArgumentException("Title is invalid")
        }
    }

    override fun toString(): String {
        return this.value
    }

    override fun equals(other: Any?): Boolean {
        return this.value == (other as Title).value
    }

    override fun hashCode(): Int {
        return this.value.hashCode()
    }
}