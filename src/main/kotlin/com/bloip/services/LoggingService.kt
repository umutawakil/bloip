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

    //TODO: Needs to be made remote and currently its not really grabbing ipAddress
    fun error(exception: Exception?, ipAddress: String?) {
        println("Error for IP: $ipAddress, error: " + exception?.message)
        exception?.printStackTrace()
    }
}