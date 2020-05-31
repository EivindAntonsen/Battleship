package no.esa.battleship.repository.mapper

import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.NoSuchShipTypeException
import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.repository.entity.ShipEntity
import no.esa.battleship.repository.entity.ShipEntity.*
import no.esa.battleship.service.domain.Component
import no.esa.battleship.service.domain.Components
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Ship

object ShipMapper {

    fun fromShipTypeIdWithParameters(id: Int,
                                     playerId: Int,
                                     shipTypeId: Int): ShipEntity {
        return when (shipTypeId) {
            1 -> Carrier(id, playerId)
            2 -> Battleship(id, playerId)
            3 -> Cruiser(id, playerId)
            4 -> Submarine(id, playerId)
            5 -> PatrolBoat(id, playerId)
            else -> throw NoSuchShipTypeException(shipTypeId)
        }
    }

    fun fromShipEntityToShip(shipEntity: ShipEntity, components: List<ComponentEntity>): Ship {
        return Ship(shipEntity.id,
             shipEntity.playerId,
             Components(ShipType.fromInt(shipEntity.shipTypeId),
                        components.map {
                            Component(it.id,
                                      it.shipId,
                                      Coordinate(it.coordinateEntity.horizontal_position,
                                                 it.coordinateEntity.vertical_position),
                                      it.isDestroyed)
                        }
             ))
    }
}
