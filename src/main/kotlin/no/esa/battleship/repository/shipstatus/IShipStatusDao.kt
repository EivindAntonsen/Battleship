package no.esa.battleship.repository.shipstatus

import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.repository.entity.ShipEntity

interface IShipStatusDao {
    fun get(shipId: Int): ShipStatus
    fun save(shipId: Int): Int
    fun update(shipId: Int, shipStatus: ShipStatus): Int
    fun getAll(playerId: Int): Map<ShipEntity, ShipStatus>
}
