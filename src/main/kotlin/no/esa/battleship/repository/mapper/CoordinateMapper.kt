package no.esa.battleship.repository.mapper

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.service.domain.Coordinate

object CoordinateMapper {

    fun toCoordinate(coordinateEntity: CoordinateEntity): Coordinate {
        return Coordinate(coordinateEntity.horizontal_position,
                          coordinateEntity.vertical_position)
    }

    fun toEntity(coordinate: Coordinate): CoordinateEntity {
        val id = coordinate.xAsInt()
                .minus(1) // to support 1-10
                .times(10) // 'a' = 1-10, 'b' = 11-20 etc
                .plus(coordinate.y) // add the Y value

        return CoordinateEntity(id, coordinate.x, coordinate.y)
    }
}
