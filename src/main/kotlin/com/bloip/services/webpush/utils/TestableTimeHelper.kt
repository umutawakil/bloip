package com.bloip.services.webpush.utils

import java.sql.Timestamp

/**
 * Created by Usman Mutawakil on 9/21/22.
 */
open class TestableTimeHelper {
    open fun getCurrentTimeMillis() : Timestamp {
        return Timestamp(System.currentTimeMillis())
    }
}

class MockTestableTimeHelper : TestableTimeHelper {
    var testTime: Long  = 0

    constructor()

    override fun getCurrentTimeMillis() : Timestamp {
        return Timestamp(this.testTime)
    }
}