package no.esa.battleship.exceptions

import no.esa.battleship.enums.ShipType
import no.esa.battleship.repository.entity.CoordinateEntity

sealed class ComponentsException(message: String) : RuntimeException(message) {

    class Alignment(horizontalCount: Int,
                    verticalCount: Int) : ComponentsException("Invalid alignment of coordinates: " +
                                                                      "$horizontalCount horizontal coordinates and " +
                                                                      "$verticalCount vertical coordinates!")
}
