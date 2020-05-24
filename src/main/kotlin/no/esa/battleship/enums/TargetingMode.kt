package no.esa.battleship.enums

import no.esa.battleship.exceptions.NoSuchTargetingModeException

enum class TargetingMode(val id: Int) {
    SEEK(1),
    DESTROY(2);

    companion object {
        fun fromInt(id: Int): TargetingMode {
            return values().firstOrNull {
                it.id == id
            } ?: throw NoSuchTargetingModeException(id)
        }
    }
}
