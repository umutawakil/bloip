package com.bloip.structures

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 9/1/22.
 */
class BumpStackTest {
    @Test
    fun verify_node_equality_conditions() {
        val nodeA: BumpStack.Node<Int, Int> = BumpStack.Node(head = null, tail = null, element = 5,  key = 1)
        val nodeB: BumpStack.Node<Int, Int> = BumpStack.Node(head = null, tail = null, element = 15, key = 1)

        assertEquals(nodeA, nodeB)
    }

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
    fun can__get__new__elements() {
        val key = 243
        val value = 445

        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        assertEquals(value, bumpStack.get(key))
    }

    @Test
    fun is_not_saving_deleted_key_info_in_other_locations() {
        val key = 243
        val value = 445
        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        bumpStack.remove(key)
        assertEquals(0, bumpStack.size())
        assertEquals(0, bumpStack.getAll().size)
    }

    @Test
    fun can__remove__element__in__unity__condition() {
        val key = 243
        val value = 445
        val bumpStack: BumpStack<Int, Int> = BumpStack()
        bumpStack.push(key, value)
        bumpStack.remove(key)
        assertEquals(0, bumpStack.size())
    }

    @Test
    fun bump_stack_node_equality() {
        val nodeA:BumpStack.Node<Long, Long> = BumpStack.Node(head = null, tail = null, element = 1, key = 1)
        val nodeB:BumpStack.Node<Long, Long> = BumpStack.Node(head = null, tail = null, element = 566, key = 1)
        assertEquals(nodeA, nodeB)
    }

    @Test
    fun can__remove__head() {
        val input = mutableListOf(1,2,3,4,5,6,7)
        val b:BumpStack<Int, Int> = BumpStack<Int, Int>()
        for(i in input.reversed()) {
            b.push(i, i)
        }

        //Remove head
        b.remove(key = 1)

        var page: BumpStack.Page<Int, Int> = b.nextPage(inputKey = null, N = input.size)
        assertEquals(input.size - 1, page.values.size)
        assertEquals(input.subList(1, input.size), page.values)
    }

    @Test
    fun can__remove__tail() {
        val input = mutableListOf(1,2,3,4,5,6,7)
        val b:BumpStack<Int, Int> = BumpStack<Int, Int>()
        for(i in input.reversed()) {
            b.push(i, i)
        }

        //Remove head
        b.remove(key = 7)

        var page: BumpStack.Page<Int, Int> = b.nextPage(inputKey = null, N = input.size)
        assertEquals(input.size - 1, page.values.size)
        assertEquals(input.subList(0, input.size-1), page.values)
    }

    @Test
    fun can__remove__central__element() {
        val input    = mutableListOf(1,2,3,4,5,6,7)
        val expected = mutableListOf(1,2,3,5,6,7)
        val b:BumpStack<Int, Int> = BumpStack<Int, Int>()
        for(i in input.reversed()) {
            b.push(i, i)
        }

        //Remove head
        b.remove(key = 4)

        var page: BumpStack.Page<Int, Int> = b.nextPage(inputKey = null, N = input.size)
        assertEquals(expected.size, page.values.size)
        assertEquals(expected, page.values)
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

    @Test
    fun CAN__UPDATE__IN__PLACE() {
        val bumpStack: BumpStack<String, Int> = BumpStack()

        bumpStack.push("A",1)
        bumpStack.push("B",2)
        bumpStack.push("C",3)
        bumpStack.push("D",4)

        bumpStack.update("C", 88)
        assertEquals(88, bumpStack.get("C"))
    }
}