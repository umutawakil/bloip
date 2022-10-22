package com.bloip.domain

import com.bloip.domain.discussion.ContentType
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Created by Usman Mutawakil on 10/21/22.
 */

//TODO: The allowed content types should be iterable and something you can list and tied back to the CDN logic that toggles on them.

class ContentTypeTest {
    @Test
    fun can__allow__mp4() {
        assertDoesNotThrow {
            ContentType("audio/mp4")
        }
    }
    @Test
    fun can__allow__webm__opus() {
        assertDoesNotThrow {
            ContentType("audio/webm; codecs=opus")
        }
    }
    @Test
    fun will__block__other__formats() {
        assertThrows <Exception> ("Should throw an exception for invalid format") {
            ContentType("audio/wav; codecs=pcm")
        }
    }
    @Test
    fun will__block__empty__string() {
        assertThrows <Exception> ("Should throw an exception for empty string") {
            ContentType("")
        }
    }
}