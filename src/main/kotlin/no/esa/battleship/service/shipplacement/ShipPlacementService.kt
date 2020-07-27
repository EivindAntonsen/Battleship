package no.esa.battleship.service.shipplacement

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.GameInitializationException.ShipPlacement
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.ShipEntity
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.utils.isHorizontallyAlignedWith
import no.esa.battleship.utils.isVerticallyAlignedWith
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ShipPlacementService(private val logger: Logger,
                           private val shipDao: IShipDao,
                           private val componentDao: IComponentDao,
                           private val shipStatusDao: IShipStatusDao,
                           private val coordinateDao: ICoordinateDao) : IShipPlacementService {

    companion object {
        private const val MAX_BOARD_LENGTH = 10
    }

    override fun placeShipsForPlayer(playerId: Int) {
        val ships = ShipType.values().map { shipType ->
            val availableCoordinates = getAvailableCoordinatesForPlayer(playerId)

            placeShip(playerId, Axis.random(), availableCoordinates, shipType)
        }

        logger.info("Placed ${ships.size} ships for player $playerId.")
    }

    private fun placeShip(playerId: Int,
                          axis: Axis,
                          availableCoordinateEntities: List<CoordinateEntity>,
                          shipType: ShipType): ShipEntity {

        val filteredCoordinates = getCoordinatesEligibleForIndexPosition(availableCoordinateEntities, axis, shipType)
        val shipComponentCoordinateOptions = getShipComponentCoordinates(filteredCoordinates, axis, shipType)

        val selectedComponentCoordinates = shipComponentCoordinateOptions.shuffled().firstOrNull { coordinateEntities ->
            availableCoordinateEntities.containsAll(coordinateEntities)
        } ?: throw ShipPlacement("Could not place ship: Unable to verify all found coordinates were available.")

        val ship = shipDao.save(playerId, shipType.id)
        componentDao.save(ship.id, selectedComponentCoordinates)
        shipStatusDao.save(ship.id)

        return ship
    }

    /**
     * Based on available coordinates, this returns a list of coordinates where
     * a ship of a given type may ultimately be placed.
     * Validation is done by ensuring the resulting list is of the same size as the ship type.
     *
     * @param availableCoordinateEntities is the list of coordinates where a ship may be placed.
     * @param axis is the plane of the ship, i.e. vertical or horizontal.
     * @param shipType is which ship type it is, i.e. Battleship, Cruiser etc.
     *                 They have different sizes, and the resulting list needs to match that.
     */
    private fun getShipComponentCoordinates(availableCoordinateEntities: List<CoordinateEntity>,
                                            axis: Axis,
                                            shipType: ShipType): List<List<CoordinateEntity>> {

        return availableCoordinateEntities.shuffled().mapNotNull { index ->
            val allowedRange = if (axis == VERTICAL) {
                index.verticalPosition until index.verticalPosition + shipType.size
            } else index.horizontalPositionAsInt() until (index.horizontalPositionAsInt() + shipType.size)

            availableCoordinateEntities.filter {
                if (axis == VERTICAL) {
                    it isVerticallyAlignedWith index && it.verticalPosition in allowedRange
                } else {
                    it isHorizontallyAlignedWith index && it.horizontalPositionAsInt() in allowedRange
                }
            }.ifEmpty {
                throw ShipPlacement("Unable to generate a list of coordinates on a $axis plane for $shipType!")
            }.takeIf {
                it.size == shipType.size
            }
        }
    }

    /**
     * When a ship is placed on the board, it is done by first placing down the rear
     * of the ship. So for a ship with 3 components facing NORTH on column A,
     * it would start with A16, then A15 and finally A14.
     *
     * This function takes a list of available coordinates, groups them by their column,
     * picks a random column, then filters out coordinates that would be too close
     * to the edge for the current ship.
     */
    private fun getCoordinatesEligibleForIndexPosition(availableCoordinateEntities: List<CoordinateEntity>,
                                                       axis: Axis,
                                                       shipType: ShipType): List<CoordinateEntity> {

        val blacklist = if (axis == VERTICAL) {
            MAX_BOARD_LENGTH until shipType.size
        } else 1..shipType.size

        return availableCoordinateEntities.filter { coordinate ->
            when (axis) {
                VERTICAL -> coordinate.verticalPosition !in blacklist
                HORIZONTAL -> coordinate.horizontalPositionAsInt() !in blacklist
            }
        }
    }

    /**
     * Returns a list of coordinates that are not occupied by any ship components.
     */
    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<CoordinateEntity> {
        val allCoordinates = coordinateDao.getAll()
        val occupiedCoordinates = shipDao.getAllShipsForPlayer(playerId).flatMap { ship ->
            componentDao.getByShipId(ship.id).map { component ->
                component.coordinateEntity
            }
        }

        return allCoordinates.filter { coordinate ->
            coordinate !in occupiedCoordinates
        }
    }
}
