package no.esa.battleship.enums

enum class Plane {
    VERTICAL,
    HORIZONTAL;

    companion object {
        fun random(): Plane {
            return values().random()
        }
    }
}
