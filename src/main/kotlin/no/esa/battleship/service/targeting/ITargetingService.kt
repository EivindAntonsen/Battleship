package no.esa.battleship.service.targeting

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity

interface ITargetingService {
    fun getTargetCoordinate(targetingEntity: TargetingEntity,
                            targetedShipEntities: List<TargetedShipEntity>): CoordinateEntity

    fun getPlayerTargeting(playerId: Int): Pair<TargetingEntity, List<TargetedShipEntity>>
}
