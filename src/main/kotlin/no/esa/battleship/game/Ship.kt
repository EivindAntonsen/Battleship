package no.esa.battleship.game

sealed class Ship {
    abstract val id: Int
    abstract val playerId: Int
    abstract val shipTypeId: Int
    abstract val components: List<ShipComponent>

    data class Carrier(override val id: Int,
                       override val playerId: Int,
                       override val components: List<ShipComponent>) : Ship() {
        override val shipTypeId: Int = 1
    }

    data class Battleship(override val id: Int,
                          override val playerId: Int,
                          override val components: List<ShipComponent>) : Ship() {
        override val shipTypeId: Int = 2
    }

    data class Cruiser(override val id: Int,
                       override val playerId: Int,
                       override val components: List<ShipComponent>) : Ship() {
        override val shipTypeId: Int = 3
    }

    data class Submarine(override val id: Int,
                         override val playerId: Int,
                         override val components: List<ShipComponent>) : Ship() {
        override val shipTypeId: Int = 4
    }

    data class PatrolBoat(override val id: Int,
                          override val playerId: Int,
                          override val components: List<ShipComponent>) : Ship() {
        override val shipTypeId: Int = 5
    }
}
