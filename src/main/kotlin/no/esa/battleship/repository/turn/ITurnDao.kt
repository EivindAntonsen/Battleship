package no.esa.battleship.repository.turn

import no.esa.battleship.repository.entity.TurnEntity

interface ITurnDao {

    fun save(gameId: Int,
             playerId: Int,
             targetPlayerId: Int,
             coordinateId: Int,
             isHit: Boolean,
             gameTurn: Int): Int

    fun getPreviousTurnsByPlayerId(playerId: Int): List<TurnEntity>
    fun getPreviousTurnsByGameId(gameId: Int): List<TurnEntity>
}
