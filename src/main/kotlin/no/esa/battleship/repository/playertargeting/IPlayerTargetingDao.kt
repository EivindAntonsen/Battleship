package no.esa.battleship.repository.playertargeting

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.service.domain.PlayerTargeting

interface IPlayerTargetingDao {
    fun find(playerId: Int): PlayerTargeting
    fun update(playerId: Int, targetingMode: TargetingMode): Int
    fun save(playerId: Int,
             targetPlayerId: Int,
             gameTurnId: Int): Int
}
