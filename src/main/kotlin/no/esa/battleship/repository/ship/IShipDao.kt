package no.esa.battleship.repository.ship

import no.esa.battleship.repository.entity.ShipEntity

interface IShipDao {
    fun find(id: Int): ShipEntity
    fun findAllShipsForPlayer(playerId: Int): List<ShipEntity>
    fun save(playerId: Int, shipTypeId: Int): ShipEntity
}
