package no.esa.battleship.resource.gameplay

import battleship.api.GamePlayApi
import battleship.model.TurnRequestDTO
import battleship.model.TurnResultDTO
import no.esa.battleship.service.gameplay.IGamePlayService
import org.springframework.http.ResponseEntity

class GameplayController(private val gamePlayService: IGamePlayService) : GamePlayApi {

    override fun executeTurn(turnRequestDTO: TurnRequestDTO): ResponseEntity<TurnResultDTO> {
        // gamePlayService.executeHumanPlayerTurn(turnRequestDTO)

        TODO()
    }
}
