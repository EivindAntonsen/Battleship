package no.esa.battleship.repository.playershipcomponent

import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.ShipComponent

interface IPlayerShipComponentDao {
    fun findByPlayerShipId(playerShipId: Int): List<ShipComponent>
    fun save(playerShipId: Int, coordinates: List<Coordinate>): List<ShipComponent>
    fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int
    fun findByGameId(gameId: Int): List<ShipComponent>
}
