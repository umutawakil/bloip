package com.bloip.sharpen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Created by Usman Mutawakil on 9/20/22.
 */
class SharpenSwordTest {

    private fun strStr(query:String, data: String) : Int {
        return indexOfHelper(qIndex = 0, dataIndex = 0, query, data)
    }

    private fun indexOfHelper(qIndex: Int, dataIndex: Int, query: String, data: String) : Int {
        println("Q: ${qIndex} D: ${dataIndex} Qv: ${query[qIndex]}, Dv: ${data[dataIndex]}")

        if(qIndex == query.length) {
            return dataIndex - query.length
        }
        if ((dataIndex == data.length) || (qIndex == query.length)) {
            return -1
        }

        if (query[qIndex] == data[dataIndex]) {
            //println(data[dataIndex])
            return indexOfHelper(qIndex = qIndex + 1, dataIndex = dataIndex + 1, query, data)
        }
        return indexOfHelper(qIndex = 0, dataIndex = dataIndex + 1, query, data)
    }

    @Test
    fun can_find_index_of_string() {
        val data = "Hello World"
        val query = "lo W"

        //assertEquals(data.indexOf(query), strStr(query, data))
    }
}