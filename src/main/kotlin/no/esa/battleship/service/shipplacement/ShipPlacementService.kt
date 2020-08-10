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

    /**
     * Places ships for a player.
     *
     * The first three are placed randomly, then the next two are placed
     * based on which coordinates score best.
     */
    override fun placeShipsForPlayer(playerId: Int) {
        val ships = ShipType.values().toList().shuffled().mapIndexed { index, shipType ->
            val availableCoordinates = getAvailableCoordinatesForPlayer(playerId)

            val placementShouldBeScored = index > 2

            placeShip(playerId,
                      Axis.random(),
                      availableCoordinates,
                      placementShouldBeScored,
                      shipType)
        }

        logger.info("Placed ${ships.size} ships for player $playerId.")
    }

    /**
     * Scores a two-dimensional list of coordinates.
     *
     * Each innermost list is a potential ship configuration, indicating where
     * each individual component of a ship can be placed. This function attempts
     * to score each such list by associating the coordinates of a ship configuration with a score.
     * The higher the score is, the more different ship configurations any given
     * coordinate can hold.
     *
     * @param shipConfigurations is a list of different ship configurations.
     */
    private fun selectShipConfigurationByScore(shipConfigurations: List<List<CoordinateEntity>>): List<CoordinateEntity>? {
        val scoreMap = mutableMapOf<CoordinateEntity, Int>().apply {
            shipConfigurations.forEach { shipConfiguration ->
                shipConfiguration.forEach { coordinateEntity ->
                    merge(coordinateEntity, 1, Integer::sum)
                }
            }
        }

        val shipConfigurationsWithScore = shipConfigurations.map { shipConfiguration ->
            val combinedScore = shipConfiguration.mapNotNull { coordinate ->
                scoreMap[coordinate]
            }.sum()

            shipConfiguration to combinedScore
        }.toMap()

        return shipConfigurationsWithScore.minBy { (_, score) ->
            score
        }?.key
    }

    private fun selectShipConfigurationByRandomSelection(shipConfigurations: List<List<CoordinateEntity>>,
                                                         availableCoordinateEntities: List<CoordinateEntity>): List<CoordinateEntity>? {
        return shipConfigurations.shuffled().firstOrNull { coordinateEntities ->
            availableCoordinateEntities.containsAll(coordinateEntities)
        }
    }

    /**
     * Place a ship on the game board.
     *
     * @param playerId is the id of the player that will have ships placed.
     * @param axis indicates which axis (i.e. horizontal or vertical) will be used.
     * @param availableCoordinateEntities is the pool from which coordinates may be placed.
     * @param scoredPlacement whether the placement is based on a score or random selection.
     * @param shipType is the type of the ship to be placed.
     *
     * @return a ship entity, as modeled in the data layer.
     */
    private fun placeShip(playerId: Int,
                          axis: Axis,
                          availableCoordinateEntities: List<CoordinateEntity>,
                          scoredPlacement: Boolean,
                          shipType: ShipType): ShipEntity {

        val filteredCoordinates = getCoordinatesEligibleForIndexPosition(availableCoordinateEntities,
                                                                         axis,
                                                                         shipType)

        val shipConfigurations = getShipConfigurationsForShipType(filteredCoordinates,
                                                                  axis,
                                                                  shipType)

        val shipConfiguration = if (scoredPlacement) {
            selectShipConfigurationByScore(shipConfigurations)
        } else selectShipConfigurationByRandomSelection(shipConfigurations,
                                                        availableCoordinateEntities)

        if (shipConfiguration.isNullOrEmpty()) {
            throw ShipPlacement("Could not place ship: Unable to verify all found coordinates were available.")
        }

        val ship = shipDao.save(playerId, shipType.id)

        componentDao.save(ship.id, shipConfiguration)
        shipStatusDao.save(ship.id)

        return ship
    }

    /**
     * Based on available coordinates, this returns a list of coordinates (ship configuration) where
     * a ship of a given type may ultimately be placed.
     * Validation is done by ensuring the resulting list is of the same size as the ship type.
     *
     * @param availableCoordinateEntities is the list of coordinates where a ship may be placed.
     * @param axis is the plane of the ship, i.e. vertical or horizontal.
     * @param shipType is which ship type it is, i.e. Battleship, Cruiser etc.
     *                 They have different sizes, and the resulting list needs to match that.
     */
    override fun getShipConfigurationsForShipType(availableCoordinateEntities: List<CoordinateEntity>,
                                                  axis: Axis,
                                                  shipType: ShipType): List<List<CoordinateEntity>> {

        return availableCoordinateEntities.shuffled().mapNotNull { indexCoordinate ->
            val startCoordinateIndex = if (axis == VERTICAL) {
                indexCoordinate.verticalPosition
            } else {
                indexCoordinate.horizontalPositionAsInt()
            }

            val finalCoordinateIndex = startCoordinateIndex + shipType.size

            val allowedRange = startCoordinateIndex until finalCoordinateIndex

            availableCoordinateEntities.filter { coordinate ->
                if (axis == VERTICAL) {
                    coordinate isVerticallyAlignedWith indexCoordinate && coordinate.verticalPosition in allowedRange
                } else {
                    coordinate isHorizontallyAlignedWith indexCoordinate && coordinate.horizontalPositionAsInt() in allowedRange
                }
            }.ifEmpty {
                throw ShipPlacement("Unable to generate a list of coordinates on a $axis plane for $shipType!")
            }.takeIf { coordinates ->
                coordinates.size == shipType.size
            }
        }
    }

    /**
     * When a ship is placed on the board, it is done by first placing down the rear
     * of the ship. So for a ship with 3 components facing NORTH on column A,
     * it would start with A10, then A9 and finally A8.
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
        val occupiedCoordinates = componentDao.getOccupiedCoordinatesByPlayerId(playerId)

        return allCoordinates.filter { coordinate ->
            coordinate !in occupiedCoordinates
        }
    }
}
