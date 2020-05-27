package no.esa.battleship.exceptions

import no.esa.battleship.enums.ShipType
import no.esa.battleship.repository.entity.CoordinateEntity

sealed class ComponentsException(message: String) : RuntimeException(message) {

    class Alignment(horizontalCount: Int,
                    verticalCount: Int) : ComponentsException("Invalid alignment of coordinates: " +
                                                                      "$horizontalCount horizontal coordinates and " +
                                                                      "$verticalCount vertical coordinates!")

    class Composition(shipType: ShipType,
                      numberOfComponents: Int) : ComponentsException("Invalid number of components ($numberOfComponents) " +
                                                                             "for ship of type $shipType: " +
                                                                             "Needs to be exactly ${shipType.size}!")

    class IntegrityViolation(c1: CoordinateEntity, c2: CoordinateEntity) : ComponentsException("Coordinates aren't adjacent: $c1 $c2")
}
