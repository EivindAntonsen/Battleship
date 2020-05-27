package no.esa.battleship.repository.component

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.ComponentEntity

interface IComponentDao {
    fun findByPlayerShipId(playerShipId: Int): List<ComponentEntity>
    fun save(playerShipId: Int, coordinateEntities: List<CoordinateEntity>): List<ComponentEntity>
    fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int
    fun findByGameId(gameId: Int): List<ComponentEntity>
}
