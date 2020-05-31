package no.esa.battleship.service.domain

data class Component(val id: Int,
                     val shipId: Int,
                     val coordinate: Coordinate,
                     val isDestroyed: Boolean)
