package com.bloip.services

import org.springframework.stereotype.Service

/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class LoggingService {
    val mode = "error"
    fun log(x: String) {
        println(x)
    }

    fun debug(x: String) {
        if (mode != "debug") {
            return
        }
        println(x)
    }

    fun error(x: String) {
        println(x)
    }
}