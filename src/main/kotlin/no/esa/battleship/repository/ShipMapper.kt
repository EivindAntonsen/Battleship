package no.esa.battleship.repository

import no.esa.battleship.exceptions.NoSuchShipTypeException
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.service.domain.Ship.*

object ShipMapper {

    fun fromShipTypeIdWithParameters(id: Int,
                                     playerId: Int,
                                     shipTypeId: Int): Ship {
        return when (shipTypeId) {
            1 -> Carrier(id, playerId)
            2 -> Battleship(id, playerId)
            3 -> Cruiser(id, playerId)
            4 -> Submarine(id, playerId)
            5 -> PatrolBoat(id, playerId)
            else -> throw NoSuchShipTypeException(shipTypeId)
        }
    }
}
