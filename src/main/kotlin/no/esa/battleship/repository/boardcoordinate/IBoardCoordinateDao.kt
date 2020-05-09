package no.esa.battleship.repository.boardcoordinate

import no.esa.battleship.service.domain.Coordinate

interface IBoardCoordinateDao {
    fun findAll(): List<Coordinate>
}
