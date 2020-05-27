package no.esa.battleship.service.domain

data class Component(val id: Int,
                     val playerShipId: Int,
                     val coordinate: Coordinate,
                     val isDestroyed: Boolean)
