package no.esa.battleship.repository.model

data class ShipComponent(val id: Int,
                         val playerShipId: Int,
                         val coordinate: Coordinate,
                         val isDestroyed: Boolean)
