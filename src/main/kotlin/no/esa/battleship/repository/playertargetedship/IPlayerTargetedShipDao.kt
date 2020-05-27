package no.esa.battleship.repository.playertargetedship

import no.esa.battleship.service.domain.PlayerTargetedShip

interface IPlayerTargetedShipDao {
    fun save(playerTargetingId: Int, playerShipId: Int): Int
    fun findByTargetingId(playerTargetingId: Int): List<PlayerTargetedShip>
}
