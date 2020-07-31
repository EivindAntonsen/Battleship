package no.esa.battleship.repository.coordinate

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.PlayerEntity

interface ICoordinateDao {
    fun getAll(): List<CoordinateEntity>
}
