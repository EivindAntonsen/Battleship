package no.esa.battleship.resource.game

import battleship.api.GameApi
import battleship.model.GameReportDTO
import no.esa.battleship.resource.mapper.GameReportMapper
import no.esa.battleship.service.gameplay.IGamePlayService
import no.esa.battleship.service.initialization.IGameInitializationService
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class GameController(private val logger: Logger,
                     private val gameInitializationService: IGameInitializationService,
                     private val gamePlayService: IGamePlayService) : GameApi {

    override fun initializeNewGame(gameSeriesId: UUID?): ResponseEntity<Int> {
        return logger.log {
            val game = gameInitializationService.initializeNewGame(gameSeriesId)

            ResponseEntity.ok(game.id)
        }
    }

    override fun playGame(gameId: Int?, gameSeriesId: UUID?): ResponseEntity<GameReportDTO> {
        return logger.log("gameId", gameId) {
            val gameReport = gamePlayService.playGame(gameId ?: gameInitializationService.initializeNewGame(gameSeriesId).id)
            val gameReportDTO = GameReportMapper.toDTO(gameReport)

            ResponseEntity.ok(gameReportDTO)
        }
    }
}
