package no.esa.battleship.repository.targeting

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.repository.entity.TargetingEntity

interface ITargetingDao {
    fun find(playerId: Int): TargetingEntity
    fun update(playerId: Int, targetingMode: TargetingMode): Int
    fun save(playerId: Int,
             targetPlayerId: Int,
             gameTurnId: Int): Int
}
