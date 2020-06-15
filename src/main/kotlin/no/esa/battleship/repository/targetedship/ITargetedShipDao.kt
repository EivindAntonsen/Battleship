package no.esa.battleship.repository.targetedship

import no.esa.battleship.repository.entity.TargetedShipEntity

interface ITargetedShipDao {
    fun save(targetingId: Int, shipId: Int): Int
    fun getByTargetingId(targetingId: Int): List<TargetedShipEntity>
    fun delete(targetingId: Int, shipId: Int): Int
}
