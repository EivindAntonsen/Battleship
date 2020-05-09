package no.esa.battleship.exceptions

import no.esa.battleship.enums.ShipDirection
import no.esa.battleship.enums.ShipType
import no.esa.battleship.service.domain.Coordinate

sealed class GameInitializationException(message: String) : RuntimeException(message) {
    class ShipPlacementException(indexCoordinate: Coordinate,
                                 shipType: ShipType,
                                 direction: ShipDirection) : GameInitializationException(
            "Error during placement of $shipType heading $direction from $indexCoordinate")

    class NoValidCoordinatesForShipPlacementException(indexCoordinate: Coordinate,
                                                      shipType: ShipType,
                                                      direction: ShipDirection,
                                                      coordinates: List<Coordinate>) : GameInitializationException(
            "Error during placement of $shipType heading $direction from $indexCoordinate: " +
                    "Coordinates for ship components have gaps in them: $coordinates.")
}
