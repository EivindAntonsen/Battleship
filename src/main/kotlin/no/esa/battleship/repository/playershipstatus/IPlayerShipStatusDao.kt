package no.esa.battleship.repository.playershipstatus

import no.esa.battleship.enums.ShipStatus

interface IPlayerShipStatusDao {
    fun find(playerShipId: Int): ShipStatus
    fun save(playerShipId: Int): Int
    fun update(playerShipId: Int, shipStatus: ShipStatus): Int
}
