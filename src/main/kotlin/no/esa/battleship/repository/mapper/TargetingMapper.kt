package no.esa.battleship.repository.mapper

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity
import no.esa.battleship.service.domain.Targeting

object TargetingMapper {

    fun toTargeting(targetingEntity: TargetingEntity, targetedShipEntities: List<TargetedShipEntity>): Targeting {
        return Targeting(targetingEntity.id,
                         targetingEntity.playerId,
                         targetingEntity.targetPlayerId,
                         TargetingMode.fromInt(targetingEntity.targetingModeId),
                         targetedShipEntities.map {
                             it.id
                         })

    }
}
