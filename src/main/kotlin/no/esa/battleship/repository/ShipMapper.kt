package no.esa.battleship.repository

import no.esa.battleship.exceptions.NoSuchShipTypeException
import no.esa.battleship.game.Ship
import no.esa.battleship.game.Ship.*
import no.esa.battleship.game.ShipComponent

object ShipMapper {

    fun fromShipTypeIdWithParameters(id: Int,
                                     playerId: Int,
                                     components: List<ShipComponent>,
                                     shipTypeId: Int): Ship {
        return when (shipTypeId) {
            1 -> Carrier(id, playerId, components)
            2 -> Battleship(id, playerId, components)
            3 -> Cruiser(id, playerId, components)
            4 -> Submarine(id, playerId, components)
            5 -> PatrolBoat(id, playerId, components)
            else -> throw NoSuchShipTypeException(shipTypeId)
        }
    }
}
