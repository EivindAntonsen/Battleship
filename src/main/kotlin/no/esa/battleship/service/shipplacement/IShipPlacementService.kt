package no.esa.battleship.service.shipplacement

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.ShipType
import no.esa.battleship.repository.entity.CoordinateEntity

interface IShipPlacementService {
    fun placeShipsForPlayer(playerId: Int)
    fun getShipConfigurationsForShipType(availableCoordinateEntities: List<CoordinateEntity>,
                                         axis: Axis,
                                         shipType: ShipType): List<List<CoordinateEntity>>
}
