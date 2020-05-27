package no.esa.battleship.repository.entity

data class ComponentEntity(val id: Int,
                           val shipId: Int,
                           val coordinateEntity: CoordinateEntity,
                           val isDestroyed: Boolean)
