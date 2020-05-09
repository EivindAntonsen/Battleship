package no.esa.battleship.service.domain

data class ShipComponent(val id: Int,
                         val playerShipId: Int,
                         val coordinate: Coordinate,
                         val isDestroyed: Boolean)
