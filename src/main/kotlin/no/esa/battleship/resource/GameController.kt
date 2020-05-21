package no.esa.battleship.resource

import no.esa.battleship.api.GameApi
import no.esa.battleship.model.ResultDTO
import no.esa.battleship.resource.mapper.ResultMapper
import no.esa.battleship.service.gameplay.IGamePlayService
import no.esa.battleship.service.initialization.IGameInitializationService
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(private val logger: Logger,
                     private val gameInitializationService: IGameInitializationService,
                     private val gamePlayService: IGamePlayService) : GameApi {

    override fun initializeNewGame(): ResponseEntity<Int> {
        return logger.log {
            val game = gameInitializationService.initializeNewGame()

            ResponseEntity.ok(game.id)
        }
    }

    override fun playGame(gameId: Int?): ResponseEntity<ResultDTO> {
        return logger.log("gameId", gameId) {
            val result = gamePlayService.playGame(gameId ?: gameInitializationService.initializeNewGame().id)
            val resultDTO = ResultMapper.toDto(result)

            ResponseEntity.ok(resultDTO)
        }
    }
}
