package no.esa.battleship.service

import no.esa.battleship.enums.Plane
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

            placeShip(playerId, Plane.random(), availableCoordinates, shipType)
        }

        logger.info("Placed ${ships.size} ships for player $playerId.")
    }

    @Retryable(ShipPlacementException::class)
    private fun placeShip(playerId: Int,
                          plane: Plane,
                          availableCoordinates: List<Coordinate>,
                          shipType: ShipType): Ship {

        val filteredCoordinates = getCoordinatesEligibleForIndexPosition(availableCoordinates,
                                                                         plane,
                                                                         shipType)
        val shipComponentCoordinateOptions = getShipComponentCoordinates(filteredCoordinates,
                                                                         plane,
                                                                         shipType)

        val selectedComponentCoordinates = shipComponentCoordinateOptions.firstOrNull { coordinates ->
            availableCoordinates.containsAll(coordinates)
        } ?: throw ShipPlacementException("Could not place ship: Unable to verify all found coordinates were available.")
        val ship = playerShipDao.save(playerId, shipType.id)
        playerShipComponentDao.save(ship.id, selectedComponentCoordinates)

        return ship
    }

    private fun getShipComponentCoordinates(availableCoordinates: List<Coordinate>,
                                            plane: Plane,
                                            shipType: ShipType): List<List<Coordinate>> {

        return availableCoordinates.shuffled().mapNotNull { index ->
            val vIndex = index.vertical_position
            val hIndex = X_Y_MAP[index.horizontal_position]!!

            val allowedRange = if (plane == VERTICAL) {
                vIndex until vIndex + shipType.size
            } else hIndex until (hIndex + shipType.size)

            availableCoordinates.filter { coordinate ->
                if (plane == VERTICAL) {
                    coordinate isHorizontallyAlignedWith index
                            && coordinate.vertical_position in allowedRange
                } else {
                    coordinate isVerticallyAlignedWith index
                            && X_Y_MAP[coordinate.horizontal_position]!! in allowedRange
                }
            }.ifEmpty {
                logger.error("Found no suitable coordinates for ship $shipType $plane from ${availableCoordinates.size} options.")

                throw ShipPlacementException("Unable to generate a list of coordinates on a $plane plane for $shipType!")
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
                                                       plane: Plane,
                                                       shipType: ShipType): List<Coordinate> {

        val blacklist = if (plane == VERTICAL) {
            (MAX_BOARD_LENGTH - shipType.size + 1)..MAX_BOARD_LENGTH
        } else 1..shipType.size

        return availableCoordinates.filter { coordinate ->
            when (plane) {
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
