package no.esa.battleship.enums

import no.esa.battleship.exceptions.NoSuchShipTypeException

enum class ShipType(val id: Int, val size: Int) {
    CARRIER(1, 5),
    BATTLESHIP(2, 4),
    CRUISER(3, 3),
    SUBMARINE(4, 3),
    PATROL_BOAT(5, 2);

    companion object {
        fun fromInt(id: Int): ShipType {
            return values().first {
                it.id == id
            }
        }
    }
}
