package no.esa.battleship.resource

import no.esa.battleship.exceptions.GameInitialization
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
        val callingClass = exception.callingClass.simpleName?.toCamelCase()
        val callingFunction = exception.callingFunction.name

        logger.error(exception.cause?.message ?: exception.message ?: exception.toString())

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(resourceBundle.getString("dataAccessException.$callingClass.$callingFunction"))
    }

    @ExceptionHandler(GameInitialization::class)
    fun handle(exception: GameInitialization): ResponseEntity<String> {
        logger.warn(exception.message)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(exception.message)
    }

    /**
     * Any other exception not covered by the above.
     */
    @ExceptionHandler(Exception::class)
    fun handle(exception: Throwable): ResponseEntity<String> {
        logger.warn(exception.message)

        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body("Something went wrong, see server logs for cause.")
    }
}
