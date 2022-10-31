package com.bloip.controllers

import com.bloip.services.LoggingService
import org.springframework.beans.factory.annotation.Autowired
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
    protected fun handleConflict(ex: Exception, request: WebRequest?): String {
        loggingService.error("Error", exception = ex)
        return "error.html"
    }
}