package no.esa.battleship.service.gameplay

import no.esa.battleship.service.domain.Result

interface IGamePlayService {
    fun playGame(gameId: Int): Result
}
