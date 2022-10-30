package com.bloip.domain

import com.bloip.domain.discussion.Title
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

/**
 * Created by Usman Mutawakil on 10/21/22.
 */
class TitleTest {
    //TODO: Need to remove bad characters
    @Test
    fun can__allow__normal__title() {
        assertDoesNotThrow {
            Title("This is a normal title")
        }
    }
    @Test
    fun will__block__long__strings__over__40() {
        val buffer = StringBuffer()
        for (i in 0 until 100) {
            buffer.append(i)
        }

        assertThrows <Exception> ("Should throw an exception for large strings") {
            Title(buffer.toString())
        }
    }
    @Test
    fun will__block__empty__string() {
        assertThrows <Exception> ("Should throw an exception for empty string") {
            Title("")
        }
    }
}