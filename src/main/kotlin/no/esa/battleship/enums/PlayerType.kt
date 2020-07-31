package no.esa.battleship.enums

import no.esa.battleship.exceptions.NoSuchPlayerTypeException

enum class PlayerType(val id: Int) {
    HUMAN(1),
    AI(2);

    companion object {
        fun fromId(id: Int): PlayerType {
            return values().firstOrNull {
                it.id == id
            } ?: throw NoSuchPlayerTypeException(id)
        }
    }
}
