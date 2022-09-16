package com.bloip.controllers

import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


/**
 * Created by Usman Mutawakil on 9/15/22.
 */
@ControllerAdvice
class ErrorHandler(
    @Autowired val loggingService: LoggingService

) : ResponseEntityExceptionHandler()

{
    @ExceptionHandler(
        value = [Exception::class]
    )

    //TODO: Needs to be made remote and currently its not really grabbing ipAddress
    protected fun handleConflict(ex: Exception?, request: WebRequest?): ResponseEntity<Any?>? {
        val bodyOfResponse = "An unexpected error/bug has occurred. Tell us on twitter @BloipApp what you were doing that caused the error."

        //TODO: remoteUser is not ip address
        loggingService.error(exception = ex, ipAddress = request?.remoteUser)

        return handleExceptionInternal(
            ex!!,
            bodyOfResponse,
            HttpHeaders(),
            HttpStatus.CONFLICT,
            request!!
        )
    }
}