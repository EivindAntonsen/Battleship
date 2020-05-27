package no.esa.battleship.repository.playershipcomponent

import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Component

interface IPlayerShipComponentDao {
    fun findByPlayerShipId(playerShipId: Int): List<Component>
    fun save(playerShipId: Int, coordinates: List<Coordinate>): List<Component>
    fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int
    fun findByGameId(gameId: Int): List<Component>
}
