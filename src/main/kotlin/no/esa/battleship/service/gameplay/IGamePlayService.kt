package no.esa.battleship.service.gameplay

import no.esa.battleship.service.domain.Player

interface IGamePlayService {
    fun playGame(gameId: Int): Player?
}
