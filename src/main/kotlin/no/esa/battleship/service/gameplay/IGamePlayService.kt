package no.esa.battleship.service.gameplay

import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.GameReport
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.domain.Result

interface IGamePlayService {
    fun playGame(gameId: Int): GameReport
    fun findRemainingPlayers(gameId: Int): List<Player>
}
