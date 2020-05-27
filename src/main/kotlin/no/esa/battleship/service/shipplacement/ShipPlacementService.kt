package no.esa.battleship.service.shipplacement

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.GameInitializationException.ShipPlacement
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playershipstatus.IPlayerShipStatusDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.utils.isVerticallyAlignedWith
import no.esa.battleship.utils.isHorizontallyAlignedWith
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class ShipPlacementService(private val logger: Logger,
                           private val playerShipDao: IPlayerShipDao,
                           private val playerShipComponentDao: IPlayerShipComponentDao,
                           private val playerShipStatusDao: IPlayerShipStatusDao,
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
                          availableCoordinates: List<Coordinate>,
                          shipType: ShipType): Ship {

        val filteredCoordinates = getCoordinatesEligibleForIndexPosition(availableCoordinates, axis, shipType)
        val shipComponentCoordinateOptions = getShipComponentCoordinates(filteredCoordinates, axis, shipType)

        val selectedComponentCoordinates = shipComponentCoordinateOptions.shuffled().firstOrNull { coordinates ->
            availableCoordinates.containsAll(coordinates)
        } ?: throw ShipPlacement("Could not place ship: Unable to verify all found coordinates were available.")

        val ship = playerShipDao.save(playerId, shipType.id)
        playerShipComponentDao.save(ship.id, selectedComponentCoordinates)
        playerShipStatusDao.save(ship.id)

        return ship
    }

    /**
     * Based on available coordinates, this returns a list of coordinates where
     * a ship of a given type may ultimately be placed.
     * Validation is done by ensuring the resulting list is of the same size as the ship type.
     *
     * @param availableCoordinates is the list of coordinates where a ship may be placed.
     * @param axis is the plane of the ship, i.e. vertical or horizontal.
     * @param shipType is which ship type it is, i.e. Battleship, Cruiser etc.
     *                 They have different sizes, and the resulting list needs to match that.
     */
    private fun getShipComponentCoordinates(availableCoordinates: List<Coordinate>,
                                            axis: Axis,
                                            shipType: ShipType): List<List<Coordinate>> {

        return availableCoordinates.shuffled().mapNotNull { index ->
            val allowedRange = if (axis == VERTICAL) {
                index.vertical_position until index.vertical_position + shipType.size
            } else index.horizontalPositionAsInt() until (index.horizontalPositionAsInt() + shipType.size)

            availableCoordinates.filter {
                if (axis == VERTICAL) {
                    it isVerticallyAlignedWith index && it.vertical_position in allowedRange
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
    private fun getCoordinatesEligibleForIndexPosition(availableCoordinates: List<Coordinate>,
                                                       axis: Axis,
                                                       shipType: ShipType): List<Coordinate> {

        val blacklist = if (axis == VERTICAL) {
            MAX_BOARD_LENGTH until shipType.size
        } else 1..shipType.size

        return availableCoordinates.filter { coordinate ->
            when (axis) {
                VERTICAL -> coordinate.vertical_position !in blacklist
                HORIZONTAL -> coordinate.horizontalPositionAsInt() !in blacklist
            }
        }
    }

    /**
     * Returns a list of coordinates that are not occupied by any ship components.
     */
    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<Coordinate> {
        val allCoordinates = coordinateDao.findAll()
        val occupiedCoordinates = playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findByPlayerShipId(ship.id).map { component ->
                component.coordinate
            }
        }

        return allCoordinates.filter { coordinate ->
            coordinate !in occupiedCoordinates
        }
    }
}
