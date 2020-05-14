package no.esa.battleship.resource

import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.utils.toCamelCase
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.*

@ControllerAdvice
class ExceptionHandler(@Qualifier("errorMessages") private val resourceBundle: ResourceBundle,
                       private val logger: Logger) {

    /**
     * All instances of this exception are thrown from data access objects,
     * and as such they hold information we do not want to show to any end user.
     *
     * Log the information we need visible in logs,
     * and return the displayed message back to the user.
     */
    @ExceptionHandler(DataAccessException::class)
    fun handle(exception: DataAccessException): ResponseEntity<String> {
        println("I should handle it.")
        val callingClass = exception.callingFunction.javaClass.enclosingClass.simpleName.toCamelCase()
        val callingFunction = exception.callingFunction.javaClass.enclosingMethod.name

        val displayedErrorMessage = resourceBundle.getString("dataAccessException.$callingClass.$callingFunction")
        val loggedErrorMessage = exception.cause?.message ?: exception.message

        logger.error(loggedErrorMessage)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(displayedErrorMessage)
    }
}
