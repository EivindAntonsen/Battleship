package no.esa.battleship.enums

import no.esa.battleship.exceptions.NoSuchShipStatusException

enum class ShipStatus(val id: Int) {
    INTACT(1),
    DESTROYED(2);

    companion object {
        fun fromId(id: Int) {
            values().firstOrNull {
                it.id == id
            } ?: throw NoSuchShipStatusException(id)
        }
    }
}
