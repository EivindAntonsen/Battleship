package no.esa.battleship.service.gameplay

import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.service.domain.GameReport
import no.esa.battleship.service.domain.TurnRequest
import no.esa.battleship.service.domain.TurnResult

interface IGamePlayService {
    fun playAiGame(gameId: Int): GameReport
    fun executeHumanPlayerTurn(turnRequest: TurnRequest): TurnResult
}
