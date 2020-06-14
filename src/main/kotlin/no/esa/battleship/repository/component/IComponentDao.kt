package no.esa.battleship.repository.component

import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.repository.entity.CoordinateEntity

interface IComponentDao {
    fun findByPlayerShipId(shipId: Int): List<ComponentEntity>
    fun save(shipId: Int, coordinateEntities: List<CoordinateEntity>): List<ComponentEntity>
    fun update(componentId: Int, isDestroyed: Boolean): Int
    fun findByGameId(gameId: Int): List<ComponentEntity>
}
