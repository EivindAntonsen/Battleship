package no.esa.battleship.repository.playershipcomponent

import no.esa.battleship.repository.model.Coordinate
import no.esa.battleship.repository.model.ShipComponent

interface IPlayerShipComponentDao {
    fun findAllComponents(playerShipId: Int): List<ShipComponent>
    fun save(playerShipId: Int, coordinate: Coordinate): Int
    fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int
}
