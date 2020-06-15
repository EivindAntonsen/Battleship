package no.esa.battleship.repository.ship

import no.esa.battleship.repository.entity.ShipEntity

interface IShipDao {
    fun get(id: Int): ShipEntity
    fun getAllShipsForPlayer(playerId: Int): List<ShipEntity>
    fun save(playerId: Int, shipTypeId: Int): ShipEntity
}
