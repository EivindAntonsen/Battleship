package no.esa.battleship.repository.boardcoordinate

import no.esa.battleship.repository.model.Coordinate

interface IBoardCoordinateDao {
    fun findAll(): List<Coordinate>
}
