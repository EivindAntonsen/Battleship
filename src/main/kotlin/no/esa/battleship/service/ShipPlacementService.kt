package no.esa.battleship.service

import no.esa.battleship.enums.Plane
import no.esa.battleship.enums.Plane.HORIZONTAL
import no.esa.battleship.enums.Plane.VERTICAL
import no.esa.battleship.enums.ShipDirection
import no.esa.battleship.enums.ShipDirection.*
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.GameInitializationException.NoValidCoordinatesForShipPlacementException
import no.esa.battleship.exceptions.InvalidGameStateException
import no.esa.battleship.repository.boardcoordinate.IBoardCoordinateDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.utils.isHorizontallyAlignedWith
import no.esa.battleship.utils.isVerticallyAlignedWith
import org.slf4j.Logger
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class ShipPlacementService(private val logger: Logger,
                           private val playerShipDao: IPlayerShipDao,
                           private val playerShipComponentDao: IPlayerShipComponentDao,
                           private val boardCoordinateDao: IBoardCoordinateDao) {

    companion object {
        private const val MAX_BOARD_LENGTH = 10
        private val COORDINATE_X_Y_MAPPING = mapOf(
                'a' to 1,
                'b' to 2,
                'c' to 3,
                'd' to 4,
                'e' to 5,
                'f' to 6,
                'g' to 7,
                'h' to 8,
                'i' to 9,
                'j' to 10)
    }

    fun placeShipsForPlayer(playerId: Int) {
        ShipType.values().map {
            val availableCoordinates = getAvailableCoordinatesForPlayer(playerId)
            val shipDirection = randomlyDecideShipDirection()

            placeShip(playerId, shipDirection, availableCoordinates, it)
        }
    }

    private fun placeShip(playerId: Int,
                          shipDirection: ShipDirection,
                          availableCoordinates: List<Coordinate>,
                          shipType: ShipType): Ship {
        val coordinatesEligibleForIndexPosition = getEligibleCoordinates(availableCoordinates,
                                                                         shipDirection,
                                                                         shipType)
        val finalShipComponentCoordinates = getShipComponentCoordinates(coordinatesEligibleForIndexPosition,
                                                                        shipDirection,
                                                                        shipType)

        return playerShipDao.save(playerId, shipType.id).also { ship ->
            playerShipComponentDao.save(ship.id, finalShipComponentCoordinates)
        }
    }

    private fun getShipComponentCoordinates(coordinates: List<Coordinate>,
                                            direction: ShipDirection,
                                            shipType: ShipType): List<Coordinate> {

        val indexPosition = coordinates.random()
        val filteredCoordinates = filterCoordinatesForIndex(coordinates,
                                                            indexPosition,
                                                            direction)
        return getFinalShipCoordinates(filteredCoordinates,
                                       indexPosition,
                                       direction,
                                       shipType)

    }

    /**
     * This filters a list of coordinates to match the direction & plane of an index coordinate.
     */
    private fun filterCoordinatesForIndex(availableCoordinates: List<Coordinate>,
                                          index: Coordinate,
                                          shipDirection: ShipDirection): List<Coordinate> {
        return availableCoordinates.filter { coordinate ->
            when (shipDirection.plane) {
                VERTICAL -> index isHorizontallyAlignedWith coordinate
                HORIZONTAL -> index isVerticallyAlignedWith coordinate
            }
        }.filter { coordinate ->
            val coordinateHorizontalAsInt = COORDINATE_X_Y_MAPPING[coordinate.horizontal_position]!!
            val indexHorizontalAsInt = COORDINATE_X_Y_MAPPING[index.horizontal_position]!!

            when (shipDirection) {
                NORTH_SOUTH -> coordinate.vertical_position >= index.vertical_position
                SOUTH_NORTH -> coordinate.vertical_position <= index.vertical_position
                WEST_EAST -> coordinateHorizontalAsInt >= indexHorizontalAsInt
                EAST_WEST -> coordinateHorizontalAsInt <= indexHorizontalAsInt
            }
        }
    }

    private fun getFinalShipCoordinates(filteredCoordinates: List<Coordinate>,
                                        index: Coordinate,
                                        direction: ShipDirection,
                                        shipType: ShipType): List<Coordinate> {

        val componentCoordinates = filteredCoordinates
                .sortedBy { it.vertical_position }
                .take(shipType.size)

        val coordinatesHaveNoGaps = noGapsInCoordinates(componentCoordinates, index, direction, shipType)

        return if (coordinatesHaveNoGaps) {
            componentCoordinates
        } else throw NoValidCoordinatesForShipPlacementException(index, shipType, direction, componentCoordinates)
    }

    private fun noGapsInCoordinates(coordinates: List<Coordinate>,
                                    index: Coordinate,
                                    direction: ShipDirection,
                                    shipType: ShipType): Boolean {
        return coordinates.all { coordinate ->
            when (direction) {
                NORTH_SOUTH -> coordinate.vertical_position in index.vertical_position..(index.vertical_position + shipType.size)
                SOUTH_NORTH -> coordinate.vertical_position in index.vertical_position..(index.vertical_position - shipType.size)
                EAST_WEST -> {
                    val finalCoordinate = COORDINATE_X_Y_MAPPING[index.horizontal_position]!! - shipType.size

                    COORDINATE_X_Y_MAPPING[coordinate.horizontal_position] in COORDINATE_X_Y_MAPPING[index.horizontal_position]!!..finalCoordinate
                }
                WEST_EAST -> {
                    val bound = COORDINATE_X_Y_MAPPING[index.horizontal_position]!! + shipType.size

                    COORDINATE_X_Y_MAPPING[coordinate.horizontal_position] in COORDINATE_X_Y_MAPPING[index.horizontal_position]!!..bound
                }
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
    private fun getEligibleCoordinates(availableCoordinates: List<Coordinate>,
                                       shipDirection: ShipDirection,
                                       shipType: ShipType): List<Coordinate> {

        return if (shipDirection.plane == VERTICAL) {
            availableCoordinates.groupBy { coordinate ->
                coordinate.horizontal_position
            }.entries.random().value.filter { coordinate ->
                when (shipDirection) {
                    SOUTH_NORTH -> coordinate.vertical_position !in 1..shipType.size
                    NORTH_SOUTH -> coordinate.vertical_position !in (MAX_BOARD_LENGTH - shipType.size..10)
                    else -> throw InvalidGameStateException("Invalid ship direction. Direction should be either north to south or the opposite.")
                }
            }
        } else {
            availableCoordinates.groupBy { coordinate ->
                coordinate.vertical_position
            }.entries.random().value.filter { coordinate ->
                when (shipDirection) {
                    EAST_WEST -> COORDINATE_X_Y_MAPPING[coordinate.horizontal_position] !in (MAX_BOARD_LENGTH - shipType.size..10)
                    WEST_EAST -> COORDINATE_X_Y_MAPPING[coordinate.horizontal_position] !in 1..shipType.size
                    else -> throw InvalidGameStateException("Invalid ship direction. Direction should be either east to west or the opposite.")
                }
            }
        }
    }

    private fun randomlyDecideShipDirection(): ShipDirection {
        val plane = randomlyDecidePlane()
        val i = Random.nextInt(0, 1)

        return when (plane) {
            VERTICAL -> if (i == 0) NORTH_SOUTH else SOUTH_NORTH
            HORIZONTAL -> if (i == 0) WEST_EAST else EAST_WEST
        }
    }

    private fun randomlyDecidePlane(): Plane = if (Random.nextInt(0, 1) == 0) VERTICAL else HORIZONTAL

    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<Coordinate> {
        val allCoordinates = boardCoordinateDao.findAll()
        val unavailableCoordinates = playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findAllComponents(ship.id).map { component ->
                component.coordinate
            }
        }

        return allCoordinates.filter { coordinate ->
            coordinate.id !in unavailableCoordinates.map { it.id }
        }
    }
}
