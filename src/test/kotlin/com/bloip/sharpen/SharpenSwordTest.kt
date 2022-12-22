package com.bloip.sharpen

import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 9/20/22.
 */
class SharpenSwordTest {
    @Test
    fun can_find_index_of_string() {
        val x = "<div>To <a href=\"XXXXXX\">unsubscribe</a> from these emails click here -> <a href=\"AXXXXXXXB\">Unsubscribe</a></div>"
        val start = "click here -> <a href=\""
        val stop  = "\">Unsubscribe</a></div>"
        val positionX = x.indexOf(start) + start.length
        val positionY = x.indexOf(stop)

        println (x.substring(positionX, positionY))
    }
}