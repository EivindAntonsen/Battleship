package no.esa.battleship.repository.turn

import no.esa.battleship.repository.entity.TurnEntity

interface ITurnDao {

    fun save(playerId: Int,
             targetPlayerId: Int,
             coordinateId: Int,
             isHit: Boolean,
             gameTurn: Int): Int

    fun getPreviousTurnsForPlayer(playerId: Int): List<TurnEntity>
}
