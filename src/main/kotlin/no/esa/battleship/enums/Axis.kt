package no.esa.battleship.enums

enum class Axis {
    VERTICAL,
    HORIZONTAL;

    companion object {
        fun random(): Axis {
            return values().random()
        }
    }
}
