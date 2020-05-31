package no.esa.battleship.repository.targetedship

import no.esa.battleship.repository.entity.TargetedShipEntity

interface ITargetedShipDao {
    fun save(playerTargetingId: Int, playerShipId: Int): Int
    fun findByTargetingId(playerTargetingId: Int): List<TargetedShipEntity>
    fun delete(targetingId: Int, shipId: Int): Int
}
