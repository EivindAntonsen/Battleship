package no.esa.battleship.service.targeting

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Targeting

interface ITargetingService {
    fun getTargetCoordinate(targeting: Targeting): Coordinate
    fun getTargeting(playerId: Int): Targeting
    fun updateTargetingMode(playerId: Int, targetingMode: TargetingMode): Int
    fun updateTargetingWithNewShipId(targetingId: Int, shipId: Int): Int
    fun removeShipIdFromTargeting(targetingId: Int, shipId: Int): Int
    fun saveInitialTargeting(playerId: Int, targetPlayerId: Int, gameTurn: Int): Int
}
