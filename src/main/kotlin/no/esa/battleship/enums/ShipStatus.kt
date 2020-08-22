package no.esa.battleship.enums

enum class ShipStatus(val id: Int) {
    INTACT(1),
    DESTROYED(2);

    companion object {
        fun fromId(id: Int): ShipStatus {
            return values().first {
                it.id == id
            }
        }
    }
}
