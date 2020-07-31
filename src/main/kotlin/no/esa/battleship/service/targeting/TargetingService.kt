package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.NoAvailableCoordinatesLeftException
import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.targetedship.ITargetedShipDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.Components
import no.esa.battleship.service.domain.ShipWithComponents
import no.esa.battleship.utils.isAdjacentWith
import no.esa.battleship.utils.validateElements
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class TargetingService(private val componentDao: IComponentDao,
                       private val shipStatusDao: IShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val shipDao: IShipDao,
                       private val targetingDao: ITargetingDao,
                       private val targetedShipDao: ITargetedShipDao,
                       private val turnDao: ITurnDao,
                       private val logger: Logger) : ITargetingService {

    override fun getTargetCoordinate(targeting: TargetingEntity): CoordinateEntity {
        return if (targeting.targetingMode == SEEK) {
            seek(targeting)
        } else destroy(targeting)
    }

    /**
     * This attempts to seek out the best coordinate for a strike, based on previous hits and misses.
     *
     * If this function is called, it means the AI effectively is guesstimating where a ship could be
     * rather than chasing down hits after an initial strike on a ship.
     *
     * @param targeting holds the information about the targeting mode, player ids etc.
     * @return The currently best ranked coordinate for a new strike (may be random if unable to score).
     */
    private fun seek(targeting: TargetingEntity): CoordinateEntity {
        val availableCoordinates = getAvailableCoordinates(targeting)
        val previousCoordinates = turnDao.getPreviousTurnsByPlayerId(targeting.playerId).map {
            it.coordinateEntity
        }
        val intactShipTypes = getIntactShipTypes(targeting)
        val scoreMap = scoreCoordinatesForShipTypes(availableCoordinates, intactShipTypes)

        return if (scoreMap.isEmpty() && availableCoordinates.isNotEmpty()) {
            logger.warn("\t\t\tplayer ${targeting.playerId} - Unable to score remaining coordinates. Selecting randomly...")

            availableCoordinates.shuffled().first()
        } else getHighestRankingCoordinate(scoreMap, previousCoordinates)
                ?: when {
                    availableCoordinates.isEmpty() ->
                        throw NoAvailableCoordinatesLeftException(this::class,
                                                                  ::seek,
                                                                  "There are no available coordinates left. Game should've ended by now!")
                    intactShipTypes.isNotEmpty() && availableCoordinates.isEmpty() ->
                        throw NoAvailableCoordinatesLeftException(this::class,
                                                                  ::seek,
                                                                  "There are intact ships remaining, but no available coordinates!")
                    scoreMap.isNotEmpty() ->
                        throw NoAvailableCoordinatesLeftException(this::class,
                                                                  ::seek,
                                                                  "Scoremap is not empty, but it still couldn't select a highest ranking coordinate!")
                    else -> throw NoAvailableCoordinatesLeftException(this::class,
                                                                      ::seek,
                                                                      "Couldn't find coordinate due to an unknown cause!")
                }
    }

    private fun getHighestRankingCoordinate(scoreMap: Map<CoordinateEntity, Int>,
                                            previousCoordinates: List<CoordinateEntity>): CoordinateEntity? {
        return scoreMap.filter { (coordinate, _) ->
            coordinate !in previousCoordinates
        }.maxBy { (_, score) ->
            score
        }?.key
    }

    private fun destroy(targeting: TargetingEntity): CoordinateEntity {
        val allCoordinates = coordinateDao.getAll()

        val previousTurns = turnDao.getPreviousTurnsByPlayerId(targeting.playerId).sortedBy { it.gameTurn }

        val previouslyAttemptedCoordinates = previousTurns.map { it.coordinateEntity }

        val availableCoordinates = allCoordinates.filter { it !in previouslyAttemptedCoordinates }

        val struckCoordinatesOnCurrentlyTargetedShips = findCurrentlyTargetedShipsWithComponents(targeting)
                .filter { ship ->
                    ship.components.any {
                        it.coordinateEntity in previouslyAttemptedCoordinates
                    }
                }.flatMap { ship ->
                    ship.components
                            .filter { it.isDestroyed }
                            .map { it.coordinateEntity }
                }

        val coordinatesAdjacentWithPreviousHits = availableCoordinates.filter { availableCoordinate ->
            struckCoordinatesOnCurrentlyTargetedShips.any { struckCoordinate ->
                struckCoordinate isAdjacentWith availableCoordinate
            }
        }

        if (coordinatesAdjacentWithPreviousHits.isEmpty()) {
            logger.warn("No available coordinates adjacent with previous hits found. Maybe all options are exhausted?")
        }

        return coordinatesAdjacentWithPreviousHits
                .shuffled()
                .firstOrNull()
                ?: throw NoValidCoordinatesException(this::class,
                                                     ::destroy,
                                                     "Found no suitable coordinates!")
    }

    private fun findCurrentlyTargetedShipsWithComponents(targeting: TargetingEntity): List<ShipWithComponents> {
        return targetedShipDao.getByTargetingId(targeting.id).map { targetedShipEntity ->
            val shipEntity = shipDao.get(targetedShipEntity.shipId)
            val componentEntities = componentDao.getByShipId(shipEntity.id)
            val shipType = ShipType.fromInt(shipEntity.shipTypeId)
            val components = Components(shipType, componentEntities)

            ShipWithComponents(shipEntity, components)
        }
    }

    private fun getIntactShipTypes(targeting: TargetingEntity): List<ShipType> {
        return shipStatusDao.getAll(targeting.targetPlayerId).filterValues { shipStatus ->
            shipStatus == ShipStatus.INTACT
        }.map { (shipEntity, _) ->
            ShipType.fromInt(shipEntity.shipTypeId)
        }
    }

    private fun combineScoreMaps(scoreMaps: List<Map<CoordinateEntity, Int>>): Map<CoordinateEntity, Int> {
        return scoreMaps.fold(mutableMapOf<CoordinateEntity, Int>()) { map, otherMap ->

            otherMap.forEach { (coordinate, score) ->
                map.merge(coordinate, score, Integer::sum)
            }

            map
        }.toMap()
    }

    private fun getAvailableCoordinates(targeting: TargetingEntity): List<CoordinateEntity> {
        val previousCoordinates = turnDao.getPreviousTurnsByPlayerId(targeting.playerId).map {
            it.coordinateEntity
        }

        return coordinateDao.getAll().filter { coordinate ->
            coordinate !in previousCoordinates
        }
    }

    private fun scoreCoordinates(availableCoordinates: List<CoordinateEntity>,
                                 shipTypes: List<ShipType>,
                                 axis: Axis): Map<ShipType, Map<CoordinateEntity, Int>> {

        if (availableCoordinates.isEmpty()) throw RuntimeException("There are no coordinates to score!")

        return shipTypes.map { shipType ->
            shipType to availableCoordinates.groupBy { coordinateEntity ->
                when (axis) {
                    VERTICAL -> coordinateEntity.verticalPosition
                    HORIZONTAL -> coordinateEntity.horizontalPositionAsInt()
                }
            }.flatMap { (_, coordinates) ->
                coordinates.sortedBy { coordinateEntity ->
                    when (axis) {
                        VERTICAL -> coordinateEntity.verticalPosition
                        HORIZONTAL -> coordinateEntity.horizontalPositionAsInt()
                    }
                }.mapIndexedNotNull { index, _ ->
                    if (index + shipType.size < coordinates.size) {
                        (index..(index + shipType.size)).map { shipComponentIndex ->
                            coordinates[shipComponentIndex]
                        }.takeIf(::coordinatesAreAdjacent)
                    } else null
                }.flatten()
            }
        }.toMap().mapValues { (_, coordinates) ->
            coordinates.groupingBy { coordinateEntity ->
                coordinateEntity
            }.eachCount()
        }
    }

    private fun scoreCoordinatesForShipTypes(availableCoordinates: List<CoordinateEntity>,
                                             shipTypes: List<ShipType>): Map<CoordinateEntity, Int> {
        val verticalScores = scoreCoordinates(availableCoordinates,
                                              shipTypes,
                                              VERTICAL).map { (_, scoreMap) -> scoreMap }
        val horizontalScores = scoreCoordinates(availableCoordinates,
                                                shipTypes,
                                                HORIZONTAL).map { (_, scoreMap) -> scoreMap }

        val verticalScoreMap = combineScoreMaps(verticalScores)
        val horizontalScoreMap = combineScoreMaps(horizontalScores)

        return combineScoreMaps(listOf(verticalScoreMap, horizontalScoreMap))
    }

    private fun coordinatesAreAdjacent(coordinates: List<CoordinateEntity>): Boolean {
        val coordinatesAreHorizontallyAligned = coordinatesAreAlignedOnAxis(coordinates, HORIZONTAL)
        val coordinatesAreVerticallyAligned = coordinatesAreAlignedOnAxis(coordinates, VERTICAL)

        return coordinatesAreHorizontallyAligned || coordinatesAreVerticallyAligned
    }

    private fun coordinatesAreAlignedOnAxis(coordinates: List<CoordinateEntity>, axis: Axis): Boolean {
        return coordinates.sortedBy { coordinate ->
            when (axis) {
                HORIZONTAL -> coordinate.horizontalPositionAsInt()
                VERTICAL -> coordinate.verticalPosition
            }
        }.validateElements { current, next -> current isAdjacentWith next }
    }

    override fun getTargeting(playerId: Int): TargetingEntity {
        return targetingDao.get(playerId)
    }

    override fun updateTargetingMode(playerId: Int, targetingMode: TargetingMode): Int {
        return targetingDao.update(playerId, targetingMode)
    }

    override fun updateTargetingWithNewShipId(targetingId: Int, shipId: Int): Int {
        return targetedShipDao.save(targetingId, shipId)
    }

    override fun removeShipIdFromTargeting(targetingId: Int, shipId: Int): Int {
        return targetedShipDao.delete(targetingId, shipId)
    }

    override fun saveInitialTargeting(playerId: Int, targetPlayerId: Int, gameTurn: Int): Int {
        return targetingDao.save(playerId, targetPlayerId, gameTurn)
    }

    override fun findTargetedShips(targetingId: Int): List<TargetedShipEntity> {
        return targetedShipDao.getByTargetingId(targetingId)
    }
}
