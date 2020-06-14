package no.esa.battleship.repository.entity

sealed class ShipEntity {
    abstract val id: Int
    abstract val playerId: Int
    abstract val shipTypeId: Int

    data class Carrier(override val id: Int,
                       override val playerId: Int) : ShipEntity() {

        override val shipTypeId: Int = 1
    }

    data class Battleship(override val id: Int,
                          override val playerId: Int) : ShipEntity() {

        override val shipTypeId: Int = 2
    }

    data class Cruiser(override val id: Int,
                       override val playerId: Int) : ShipEntity() {

        override val shipTypeId: Int = 3
    }

    data class Submarine(override val id: Int,
                         override val playerId: Int) : ShipEntity() {

        override val shipTypeId: Int = 4
    }

    data class PatrolBoat(override val id: Int,
                          override val playerId: Int) : ShipEntity() {

        override val shipTypeId: Int = 5
    }
}
