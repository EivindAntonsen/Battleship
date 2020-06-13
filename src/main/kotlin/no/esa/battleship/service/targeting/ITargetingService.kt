package no.esa.battleship.service.targeting

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity

interface ITargetingService {
    fun getTargetCoordinate(targeting: TargetingEntity): CoordinateEntity
    fun getTargeting(playerId: Int): TargetingEntity
    fun updateTargetingMode(playerId: Int, targetingMode: TargetingMode): Int
    fun updateTargetingWithNewShipId(targetingId: Int, shipId: Int): Int
    fun removeShipIdFromTargeting(targetingId: Int, shipId: Int): Int
    fun saveInitialTargeting(playerId: Int, targetPlayerId: Int, gameTurn: Int): Int
    fun findTargetedShips(targetingId: Int): List<TargetedShipEntity>
}
