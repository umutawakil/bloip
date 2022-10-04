package com.bloip.domain.discussion

import com.bloip.domain.ValueObject
import java.net.URL
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * Created by Usman Mutawakil on 10/3/22.
 */

@Embeddable
class YoutubeLink : ValueObject {
    @Column(name = "youtube_link")
    @NotNull
    @Size(min = 10, max = 100, message = "Youtube link is too long must be between 10 and 100 characters")
    val value: String

    constructor(value: String) {
        if(isYoutubeLink(input = value)) {
            this.value = value
            validate()
        } else {
            throw RuntimeException("Invalid Youtube link")
        }
    }

    private fun isYoutubeLink(input: String) : Boolean {
        val url = URL(input)
        return (url.protocol == "http" || url.protocol == "https") && (
                    url.host == "youtube.com" || url.host == "youtu.be"
                    || url.host == "www.youtube.com" || url.host === "www.youtu.be"
                    || url.host == "m.youtube.com" || url.host == "m.youtu.be"
                )
    }

    override fun toString(): String {
        return this.value
    }
}