package com.bloip.controllers.error

import com.bloip.services.LoggingService
import com.bloip.services.admin.AdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Created by Usman Mutawakil on 9/15/22.
 */
@ControllerAdvice
class ErrorHandler(
    @Autowired val loggingService: LoggingService,
    @Autowired val adminService: AdminService

) : ResponseEntityExceptionHandler()

{
    @ExceptionHandler(
        value = [Exception::class]
    )
    protected fun handleConflict(ex: Exception, request: WebRequest?): String {
        loggingService.log("Global handler called")

        if(ex is org.springframework.security.access.AccessDeniedException) {
            val principal: UserDetails = SecurityContextHolder.getContext().authentication.principal as UserDetails
            val message = "Unauthorized user attempting to access protected page: " +principal.username
            loggingService.log(message)
             adminService.recordEvent(message)

        } else {
            loggingService.error("Error", exception = ex)
            adminService.recordException(exception = ex)
        }

        return "error/generic-error.html"
    }
}