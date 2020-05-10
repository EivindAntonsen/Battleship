package no.esa.battleship.service

import no.esa.battleship.enums.Direction
import no.esa.battleship.enums.Direction.*
import no.esa.battleship.enums.Plane.HORIZONTAL
import no.esa.battleship.enums.Plane.VERTICAL
import no.esa.battleship.enums.ShipType
import no.esa.battleship.exceptions.GameInitializationException.ShipPlacementException
import no.esa.battleship.repository.boardcoordinate.IBoardCoordinateDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.utils.isHorizontallyAlignedWith
import no.esa.battleship.utils.isVerticallyAlignedWith
import org.slf4j.Logger
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class ShipPlacementService(private val logger: Logger,
                           private val playerShipDao: IPlayerShipDao,
                           private val playerShipComponentDao: IPlayerShipComponentDao,
                           private val boardCoordinateDao: IBoardCoordinateDao) {

    companion object {
        private const val MAX_BOARD_LENGTH = 10
        private val X_Y_MAP = mapOf(
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
        val ships = ShipType.values().map { shipType ->
            val availableCoordinates = getAvailableCoordinatesForPlayer(playerId)

            placeShip(playerId, Direction.random(), availableCoordinates, shipType)
        }

        logger.info("Placed ${ships.size} ships for player $playerId.")
    }

    @Retryable(ShipPlacementException::class)
    private fun placeShip(playerId: Int,
                          direction: Direction,
                          availableCoordinates: List<Coordinate>,
                          shipType: ShipType): Ship {

        val coordinatesEligibleForIndexPosition = getCoordinatesEligibleForIndexPosition(availableCoordinates,
                                                                                         direction,
                                                                                         shipType)
        val shipComponentCoordinateOptions = getShipComponentCoordinates(coordinatesEligibleForIndexPosition,
                                                                         direction,
                                                                         shipType)

        val selectedComponents = shipComponentCoordinateOptions.first()
        val ship = playerShipDao.save(playerId, shipType.id)
        playerShipComponentDao.save(ship.id, selectedComponents)

        return ship
    }

    private fun getShipComponentCoordinates(coordinates: List<Coordinate>,
                                            direction: Direction,
                                            shipType: ShipType): List<List<Coordinate>> {

        return coordinates.shuffled().map { index ->
            val filteredCoordinates = filterCoordinatesToFitWithIndex(coordinates, index, direction)
            val componentCoordinates = getFinalShipCoordinates(filteredCoordinates, index, direction, shipType)

            if (componentCoordinates.isNotEmpty()) componentCoordinates else emptyList()
        }.filter {
            it.size == shipType.size
        }.ifEmpty {
            throw ShipPlacementException("Unable to generate a list of coordinates for " +
                                                 "$shipType heading $direction!")
        }
    }

    /**
     * This filters a list of coordinates to match the direction & plane of an index coordinate.
     */
    private fun filterCoordinatesToFitWithIndex(availableCoordinates: List<Coordinate>,
                                                index: Coordinate,
                                                direction: Direction): List<Coordinate> {
        return availableCoordinates.filter { coordinate ->
            when (direction.plane) {
                VERTICAL -> index isHorizontallyAlignedWith coordinate
                HORIZONTAL -> index isVerticallyAlignedWith coordinate
            }
        }.filter { coordinate ->
            when (direction) {
                NORTH_SOUTH -> coordinate.vertical_position >= index.vertical_position
                SOUTH_NORTH -> coordinate.vertical_position <= index.vertical_position
                WEST_EAST -> X_Y_MAP[coordinate.horizontal_position]!! >= X_Y_MAP[index.horizontal_position]!!
                EAST_WEST -> X_Y_MAP[coordinate.horizontal_position]!! <= X_Y_MAP[index.horizontal_position]!!
            }
        }
    }

    private fun getFinalShipCoordinates(filteredCoordinates: List<Coordinate>,
                                        index: Coordinate,
                                        direction: Direction,
                                        shipType: ShipType): List<Coordinate> {

        val componentCoordinates = filteredCoordinates
                .sortedWith(coordinateComparator(direction))
                .take(shipType.size)

        val noGapsInCoordinates = noGapsInCoordinates(componentCoordinates, index, direction, shipType)
        val sizeFitsShip = componentCoordinates.size == shipType.size
        val requirements = listOf(noGapsInCoordinates, sizeFitsShip)

        return if (requirements.all { true }) {
            componentCoordinates
        } else emptyList()
    }

    private fun coordinateComparator(direction: Direction): Comparator<Coordinate> {
        return Comparator { first, second ->
            if (direction.plane == VERTICAL) {
                when {
                    first.vertical_position > second.vertical_position -> if (direction == NORTH_SOUTH) 1 else -1
                    first.vertical_position < second.vertical_position -> if (direction == NORTH_SOUTH) -1 else 1
                    else -> 0
                }
            } else {
                when {
                    first.horizontal_position > second.horizontal_position -> if (direction == WEST_EAST) 1 else -1
                    first.horizontal_position > second.horizontal_position -> if (direction == WEST_EAST) -1 else 1
                    else -> 0
                }
            }
        }
    }

    private fun noGapsInCoordinates(coordinates: List<Coordinate>,
                                    index: Coordinate,
                                    direction: Direction,
                                    shipType: ShipType): Boolean {

        return coordinates.all { coordinate ->
            when (direction) {
                NORTH_SOUTH -> coordinate.vertical_position in index.vertical_position..(index.vertical_position + shipType.size)
                SOUTH_NORTH -> coordinate.vertical_position in index.vertical_position..(index.vertical_position - shipType.size)
                EAST_WEST -> {
                    val finalCoordinate = X_Y_MAP[index.horizontal_position]!! - shipType.size

                    X_Y_MAP[coordinate.horizontal_position] in X_Y_MAP[index.horizontal_position]!!..finalCoordinate
                }
                WEST_EAST -> {
                    val bound = X_Y_MAP[index.horizontal_position]!! + shipType.size

                    X_Y_MAP[coordinate.horizontal_position] in X_Y_MAP[index.horizontal_position]!!..bound
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
    private fun getCoordinatesEligibleForIndexPosition(availableCoordinates: List<Coordinate>,
                                                       direction: Direction,
                                                       shipType: ShipType): List<Coordinate> {

        val blacklist = if (direction == NORTH_SOUTH || direction == WEST_EAST) {
            (MAX_BOARD_LENGTH - shipType.size + 1)..MAX_BOARD_LENGTH
        } else 1..shipType.size

        return availableCoordinates.filter { coordinate ->
            when (direction.plane) {
                VERTICAL -> coordinate.vertical_position !in blacklist
                HORIZONTAL -> X_Y_MAP[coordinate.horizontal_position] !in blacklist
            }
        }
    }

    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<Coordinate> {
        val allCoordinates = boardCoordinateDao.findAll()
        val occupiedCoordinates = playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findAllComponents(ship.id).map { component ->
                component.coordinate
            }
        }

        return allCoordinates.filter { coordinate ->
            coordinate.id !in occupiedCoordinates.map { it.id }
        }
    }
}
