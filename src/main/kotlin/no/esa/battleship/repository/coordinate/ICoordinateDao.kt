package no.esa.battleship.repository.coordinate

import no.esa.battleship.service.domain.Coordinate

interface ICoordinateDao {
    fun findAll(): List<Coordinate>
}
