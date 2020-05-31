package no.esa.battleship.repository.mapper

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.service.domain.Coordinate

object CoordinateMapper {

    fun toCoordinate(coordinateEntity: CoordinateEntity): Coordinate {
        return Coordinate(coordinateEntity.horizontal_position,
                          coordinateEntity.vertical_position)
    }

    fun toEntity(coordinate: Coordinate): CoordinateEntity {
        val id = coordinate.xAsInt().times(10).plus(coordinate.y)

        return CoordinateEntity(id, coordinate.x, coordinate.y)
    }
}
