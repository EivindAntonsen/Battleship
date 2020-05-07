package no.esa.battleship.repository.playership

import no.esa.battleship.game.Ship

interface IPlayerShipDao {
    fun find(id: Int)
    fun save(ship: Ship): Int
}
