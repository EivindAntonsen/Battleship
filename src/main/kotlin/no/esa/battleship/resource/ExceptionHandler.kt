package no.esa.battleship.resource

import no.esa.battleship.repository.exceptions.DataAccessException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandler {

    /**
     * All instances of this exception are thrown from data access objects,
     * and as such they hold information we do not want to show to any end user.
     *
     * Log the information we need visible in logs,
     * and return the displayed message back to the user.
     */
    @ExceptionHandler(DataAccessException::class)
    fun handle(exception: DataAccessException): ResponseEntity<String> {
        val className = exception.function.javaClass.enclosingClass.simpleName
        val logger = LoggerFactory.getLogger(className)

        val loggedErrorMessage = exception.cause?.message ?: exception.message
        val displayedErrorMessage = exception.message

        logger.error(loggedErrorMessage)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(displayedErrorMessage)
    }
}
