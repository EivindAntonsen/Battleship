package no.esa.battleship.resource

import no.esa.battleship.exceptions.InvalidGameStateException
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

    /**
     * These errors occur when the state of the game is invalid.
     *
     * Examples (more may exist):
     *   1. Games with more than 2 players.
     *   2. Players with more than 5 ships.
     *   3. Player ships that overlap.
     *   4. Ships with more than 5 components.
     *   5. Games that haven't concluded after 100 turns etc (only 100 tiles in game board).
     */
    @ExceptionHandler(InvalidGameStateException::class)
    fun handle(exception: InvalidGameStateException): ResponseEntity<String> {
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
