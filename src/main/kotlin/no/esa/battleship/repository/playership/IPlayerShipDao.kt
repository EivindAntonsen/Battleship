package no.esa.battleship.repository.playership

import no.esa.battleship.service.domain.Ship

interface IPlayerShipDao {
    fun find(id: Int): Ship
    fun findAllShipsForPlayer(playerId: Int): List<Ship>
    fun save(playerId: Int, shipTypeId: Int): Ship
}
