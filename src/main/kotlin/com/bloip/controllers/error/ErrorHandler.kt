package com.bloip.controllers.error
import com.bloip.exceptions.ExcessUserEmailsException
import com.bloip.services.LoggingService
import com.bloip.services.admin.AdminService
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
    @Autowired val adminService: AdminService,
    @Autowired val loggingService: LoggingService

) : ResponseEntityExceptionHandler()

{
    @ExceptionHandler(
        value = [Exception::class]
    )
    protected fun handleConflict(ex: Exception, request: WebRequest?): String {
        //This is here because the admin service doesn't always run depending on the environment
        ex.printStackTrace()

        adminService.recordException(exception = ex)

        return "error/generic-error.html"
    }

    @ExceptionHandler(
        value = [ExcessUserEmailsException::class]
    )
    protected fun handleExcessEmails(ex: Exception, request: WebRequest?): String {
        loggingService.log("Excessive emails sent for one user")
        return "error/excessive-emails.html"
    }
}