package com.bloip.structures

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import java.util.*

/**
 * Created by Usman Mutawakil on 9/1/22.
 */
class BumpStackTest {
    @Test
    fun can__Push__New__Elements() {
        val key = 243
        val value = 445

        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        assertEquals(value, bumpStack.get(key))
    }

    //You can only add elements to a bump stack once.
    @Test
    fun can__prevent_multiple_pushes() {
        var exception: Exception? = null
        try {
            val key = 243
            val value = 445

            val bumpStack: BumpStack<Int, Int> = BumpStack()
            bumpStack.push(key, value)
            bumpStack.push(key, 5)
            bumpStack.push(key, 5)
        } catch (testException:Exception) {
            exception = testException
        }
        assertNotNull(exception)
    }

    @Test
    fun can__Get__New__Elements() {
        val key = 243
        val value = 445

        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        assertEquals(value, bumpStack.get(key))
    }

    @Test
    fun can__Remove__Elements() {
        val key = 243
        val value = 445
        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        bumpStack.push(value, value)
        assertEquals(value, bumpStack.get(key))

        bumpStack.remove(key)
        assertEquals(null, bumpStack.get(key))
        assertEquals(bumpStack.size(), 1)
    }

    @Test
    fun can__Return__Correct__Size() {
        val n = 100
        /** Resize from push **/
        val bumpStack: BumpStack<Int, Int> = BumpStack()
        for(i in 0 until n) {
            bumpStack.push(i, i)
        }
        assertEquals(100, bumpStack.size())

        /**Resize from remove **/
        for(i in 0 until n) {
            bumpStack.remove(i)
        }
        assertEquals(0, bumpStack.size())
    }

    @Test
    fun single__discussion__test__nextPage() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("i")

        for (i in input) {
            bumpStack.push(i, i)
        }

        var page: BumpStack.Page<String, String> = bumpStack.nextPage("i", 5)
        assertNull(page.previousOffsetKey)
        assertNull( page.nextOffsetKey)
    }

    @Test
    fun can__retrieve_full_input() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("a","b","c","d","e","g","h","i")

        for (i in input) {
            bumpStack.push(i, i)
        }
        val expectedValues: List<String> = input.reversed()
        var elements: List<String> = bumpStack.getAll()

        assertEquals(input.size, elements.size)
        for (i in expectedValues.indices) {
            assertEquals(expectedValues[i], elements[i])
        }
    }

    @Test
    fun bump_preserves_length_on_unit_input() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("a")

        for (i in input) {
            bumpStack.push(i, i)
        }
        bumpStack.bump(key = "a")
        var elements: List<String> = bumpStack.getAll()
        assertEquals(input.size, elements.size)
        assertEquals(input[0], elements[0])
    }

    @Test
    fun bump_preserves_length_on_multi_input() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("a","b")

        for (i in input) {
            bumpStack.push(i, i)
        }

        bumpStack.bump(key = "a")

        var elements: List<String> = bumpStack.getAll()
        assertEquals(input.size, elements.size)
        assertEquals(input[0], elements[0])
    }


    @Test
    fun can__Bump__Elements__In__LIFO__Order() {
        val bumpStack: BumpStack<Int, Int> = BumpStack()
        for (i in 0..20) {
            val v = i * 2
            bumpStack.push(i, v)
        }
        bumpStack.bump(5)
        assertEquals(10, bumpStack.nextPage(null, 5).values[0])

        bumpStack.bump(10)
        assertEquals(20, bumpStack.nextPage(null, 5).values[0])

        bumpStack.bump(0)
        assertEquals(0, bumpStack.nextPage(null, 5).values[0])
        assertEquals(21, bumpStack.size())

        val results = bumpStack.nextPage(null, 5).values

        assertEquals(0, results[0])
        assertEquals(20, results[1])
        assertEquals(10, results[2])

        val results2 = bumpStack.previousPage(5, 3).values

        assertEquals(0,  results2[0])
        assertEquals(20, results2[1])
        assertEquals(10,  results2[2])
    }

    /** Pagination **/
    @Test
    fun can_detect_empty_previous_and_next_using_NextPage() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("d","c","b","a")

        for (i in input) {
            bumpStack.push(i, i)
        }

        var page: BumpStack.Page<String, String> = bumpStack.nextPage("a", 5)
        assertNull(page.previousOffsetKey)
        assertNull(page.nextOffsetKey)
    }
    @Test
    fun can_detect_empty_previous_and_next_using_PreviousPage() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("a","b","c","d")

        for (i in input) {
            bumpStack.push(i, i)
        }

        var page: BumpStack.Page<String, String> = bumpStack.previousPage("a", 5)
        assertNull(page.previousOffsetKey)
        assertNull(page.nextOffsetKey)
    }
    @Test
    fun can_detect_previous_and_next_using_NextPage() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("d","c","b","a")

        for (i in input) {
            bumpStack.push(i, i)
        }

        var page: BumpStack.Page<String, String> = bumpStack.nextPage("b", 2)
        assertEquals("a", bumpStack.get(page.previousOffsetKey!!))
        assertEquals("d", bumpStack.get(page.nextOffsetKey!!))
        assertEquals(listOf("b","c"), page.values)
        assertEquals(2, page.values.size)
    }
    @Test
    fun can_detect_previous_and_next_using_PreviousPage() {
        val bumpStack: BumpStack<String, String> = BumpStack()
        val input: MutableList<String> = mutableListOf("d","c","b","a")

        for (i in input) {
            bumpStack.push(i, i)
        }

        var page: BumpStack.Page<String, String> = bumpStack.previousPage("c", 2)
        assertEquals("a", bumpStack.get(page.previousOffsetKey!!))
        assertEquals("d", bumpStack.get(page.nextOffsetKey!!))
        assertEquals(listOf("b","c"), page.values)
        assertEquals(2, page.values.size)
    }
}