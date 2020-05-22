package no.esa.battleship.repository.playerturn

import no.esa.battleship.service.domain.PlayerTurn

interface IPlayerTurnDao {

    fun save(playerId: Int,
             coordinateId: Int,
             isHit: Boolean,
             gameTurn: Int): Int

    fun getPreviousTurnsForPlayer(playerId: Int): List<PlayerTurn>
}
