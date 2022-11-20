package com.bloip.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * Created by Usman Mutawakil on 6/28/22.
 */
@Service
class LoggingService {
    var logger: Logger = LoggerFactory.getLogger(LoggingService::class.java)

    fun log(x: String) {
        logger.info(x)
    }

    fun debug(x: String) {
        logger.debug(x)
    }

    fun error(x: String) {
        logger.error(x)
    }

    //TODO: Needs to be made remote and currently its not really grabbing ipAddress
    fun error(exception: Exception?, ipAddress: String?) {
        logger.error("Error for IP: $ipAddress, error: " + exception?.message)
        exception?.printStackTrace()
    }

    fun error(devMessage: String, exception: Exception) {
        logger.error(devMessage +" " + exception.message)
        exception.printStackTrace()
    }
}