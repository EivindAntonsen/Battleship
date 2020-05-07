package no.esa.battleship.repository.boardcoordinate

import no.esa.battleship.game.Coordinate

interface IBoardCoordinateDao {
    fun findAll(): List<Coordinate>
}
