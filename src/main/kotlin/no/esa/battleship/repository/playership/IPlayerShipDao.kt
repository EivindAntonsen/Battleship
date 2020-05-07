package no.esa.battleship.repository.playership

import no.esa.battleship.repository.model.Ship

interface IPlayerShipDao {
    fun find(id: Int): Ship
    fun save(ship: Ship): Int
}
