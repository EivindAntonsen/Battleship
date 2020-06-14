package no.esa.battleship.resource.game

import battleship.api.GameApi
import battleship.model.GameReportDTO
import no.esa.battleship.annotation.Logged
import no.esa.battleship.resource.mapper.GameReportMapper
import no.esa.battleship.service.gameplay.IGamePlayService
import no.esa.battleship.service.initialization.IGameInitializationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class GameController(private val gameInitializationService: IGameInitializationService,
                     private val gamePlayService: IGamePlayService) : GameApi {

    @Logged
    override fun initializeNewGame(gameSeriesId: UUID?): ResponseEntity<Int> {
        val game = gameInitializationService.initializeNewGame(gameSeriesId)

        return ResponseEntity.ok(game.id)
    }

    @Logged
    override fun playGame(gameId: Int?, gameSeriesId: UUID?): ResponseEntity<GameReportDTO> {
        val gameReport = gamePlayService.playGame(gameId ?: gameInitializationService.initializeNewGame(gameSeriesId).id)
        val gameReportDTO = GameReportMapper.toDTO(gameReport)

        return ResponseEntity.ok(gameReportDTO)
    }
}
