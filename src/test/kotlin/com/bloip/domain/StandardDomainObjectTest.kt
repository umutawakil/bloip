package com.bloip.domain

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 9/13/22.
 */
class StandardDomainObjectTest {
    private class TestObject : StandardDomainObject {
        override var id: Long

        constructor(id: Long) {
            this.id = id
        }
    }

    @Test
    fun Can__determine__equality__by__ID__value() {
        val standardDomainObjectA = StandardDomainObject()
        val standardDomainObjectB = StandardDomainObject()

        /** At the moment this should only be true because objects are defaulted to an ID of zero when created.
         * This is changed after they are persisted.
         * **/
        Assertions.assertEquals(standardDomainObjectA, standardDomainObjectB)

        Assertions.assertNotEquals(TestObject(5), TestObject(6))
        Assertions.assertEquals(TestObject(3), TestObject(3))
    }

}