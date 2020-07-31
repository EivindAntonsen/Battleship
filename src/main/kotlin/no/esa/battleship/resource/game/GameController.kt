package no.esa.battleship.resource.game

import battleship.api.GameApi
import battleship.model.GameReportDTO
import no.esa.battleship.resource.mapper.GameReportMapper
import no.esa.battleship.service.gameplay.IGamePlayService
import no.esa.battleship.service.initialization.IGameInitializationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class GameController(private val gameInitializationService: IGameInitializationService,
                     private val gamePlayService: IGamePlayService) : GameApi {

    override fun initializeNewGame(onlyAI: Boolean): ResponseEntity<Int> {
        val game = gameInitializationService.initializeNewGame(onlyAI)

        return ResponseEntity.ok(game.id)
    }

    @CrossOrigin(origins = ["http://localhost:3000"])
    override fun playAiGame(gameId: Int?): ResponseEntity<GameReportDTO> {
        val gameReport = gamePlayService.playGame(gameId ?: gameInitializationService.initializeNewGame(true).id)
        val gameReportDTO = GameReportMapper.toDTO(gameReport)

        return ResponseEntity.ok(gameReportDTO)
    }
}
