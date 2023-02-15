package com.bloip.sharpen

import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 2/12/23.
 */
class GenericTest {
    @Test
    fun testSomething() {
        var o: JSONObject = JSONObject()
        o = o.put("discussionId", "discussionId")
        o = o.put("trackNumber", "trackNumber")
        o = o.put("fileName", "fileName")

        println("JSONObject: "+o)
        val messageBody: String = o.toString()
        println("JSON MESSAGE: $messageBody")
    }
}