package com.bloip.sandbox

import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Just random area to test anything
 */
class SandboxTest {
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