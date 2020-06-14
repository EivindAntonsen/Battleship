package no.esa.battleship.repository.coordinate

import no.esa.battleship.repository.entity.CoordinateEntity

interface ICoordinateDao {
    fun findAll(): List<CoordinateEntity>
}
