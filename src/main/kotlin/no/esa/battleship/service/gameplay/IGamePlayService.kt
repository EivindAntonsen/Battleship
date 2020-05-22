package no.esa.battleship.service.gameplay

import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.domain.Result

interface IGamePlayService {
    fun playGame(gameId: Int): Result
    fun getTargetCoordinate(playerId: Int): Coordinate
    fun findRemainingPlayers(gameId: Int): List<Player>
    fun calculateProbableMinimumDistance(playerId: Int): Int
}
