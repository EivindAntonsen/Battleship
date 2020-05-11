package no.esa.battleship.repository.boardcoordinate

import no.esa.battleship.service.domain.Coordinate

interface ICoordinateDao {
    fun findAll(): List<Coordinate>
}
