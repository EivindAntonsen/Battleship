package no.esa.battleship.repository.component

import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.PlayerEntity

interface IComponentDao {
    fun getByShipId(shipId: Int): List<ComponentEntity>
    fun save(shipId: Int, coordinates: List<CoordinateEntity>): List<ComponentEntity>
    fun update(componentId: Int, isDestroyed: Boolean): Int
    fun getByGameId(gameId: Int): List<ComponentEntity>
    fun findRemainingPlayersByGameId(gameId: Int): List<PlayerEntity>
    fun getOccupiedCoordinatesByPlayerId(playerId: Int): List<CoordinateEntity>
}
