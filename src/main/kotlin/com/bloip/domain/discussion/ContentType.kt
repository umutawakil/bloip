package com.bloip.domain.discussion

import com.bloip.domain.ValueObject
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern

/**
 * Created by Usman Mutawakil on 10/21/22.
 */
@Embeddable
class ContentType : ValueObject {
    @Column(name = "content_type")
    @NotNull
    @Pattern(regexp = "audio/mp4|audio/webm; codecs=opus", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val value: String

    constructor(value: String) {
        this.value = value
        validate()
    }

    override fun toString(): String {
        return this.value
    }
}