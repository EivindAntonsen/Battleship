package no.esa.battleship.repository.playershipstatus

import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.service.domain.Ship

interface IPlayerShipStatusDao {
    fun find(playerShipId: Int): ShipStatus
    fun save(playerShipId: Int): Int
    fun update(playerShipId: Int, shipStatus: ShipStatus): Int
    fun findAll(playerId: Int): Map<Ship, ShipStatus>
}
