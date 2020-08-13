package no.esa.battleship.resource.exception

import no.esa.battleship.exceptions.GameStateException
import no.esa.battleship.repository.exceptions.DataAccessException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.util.*
import javax.validation.ConstraintViolationException

@ControllerAdvice
class ExceptionHandler(@Qualifier("errorMessages") private val resourceBundle: ResourceBundle,
                       private val exceptionHandlerLogger: Logger) {

    /**
     * All instances of this exception are thrown from data access objects,
     * and as such they hold information we do not want to show to any end user.
     *
     * Log the information we need visible in logs,
     * and return the displayed message back to the user.
     */
    @ExceptionHandler(DataAccessException::class)
    fun handle(exception: DataAccessException): ResponseEntity<String> {
        val callingClass = exception.callingClass.simpleName?.decapitalize()
        val callingFunction = exception.callingFunction.name
        val loggedErrorMessage = exception.cause?.message
                ?: exception.message
                ?: exception.toString()

        val logger = callingClass?.let {
            LoggerFactory.getLogger(callingClass)
        } ?: exceptionHandlerLogger

        logger.error(loggedErrorMessage)

        val displayedErrorMessage = resourceBundle.getString("dataAccessException.$callingClass.$callingFunction")

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(displayedErrorMessage)
    }

    @ExceptionHandler(ConstraintViolationException::class,
                      IllegalArgumentException::class)
    fun handle(exception: ConstraintViolationException): ResponseEntity<String> {
        exceptionHandlerLogger.warn(exception.message)

        return ResponseEntity
                .status(BAD_REQUEST)
                .body(exception.message)
    }

    /**
     * Logs an exception using the logger of the class of the called function,
     * before returning a response entity with a generic exception.
     */
    @ExceptionHandler(GameStateException::class)
    fun handle(exception: GameStateException): ResponseEntity<String> {
        val logger = LoggerFactory.getLogger(exception.callingClass.qualifiedName)

        logger.warn(exception.message)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body("Invalid game state, check server logs for cause.")
    }

    /**
     * Any other exception not covered by the above.
     */
    @ExceptionHandler(Exception::class)
    fun handle(exception: Throwable): ResponseEntity<String> {
        exceptionHandlerLogger.warn(exception.message)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body("Something went wrong, see server logs for cause.")
    }
}
