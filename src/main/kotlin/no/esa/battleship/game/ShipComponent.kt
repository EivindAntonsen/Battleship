package no.esa.battleship.game

data class ShipComponent(val id: Int,
                         val playerShipId: Int,
                         val coordinate: Coordinate,
                         val isDestroyed: Boolean)
