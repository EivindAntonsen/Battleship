package no.esa.battleship.resource

import no.esa.battleship.service.GameInitializationService
import no.esa.battleship.api.GameApi
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(private val logger: Logger,
                     private val gameInitializationService: GameInitializationService) : GameApi {

    override fun initializeNewGame(): ResponseEntity<String> {
        return try {
            val game = gameInitializationService.initializeNewGame()

            ResponseEntity.ok("Game with id ${game.id} created.")
        } catch (error: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to initialize game: ${error.message}.")
        }
    }
}
