package no.esa.battleship.service.targeting

import no.esa.battleship.enums.*
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.NoAvailableCoordinatesLeftException
import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity
import no.esa.battleship.repository.entity.TurnEntity
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.targetedship.ITargetedShipDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.Components
import no.esa.battleship.service.domain.ShipWithComponents
import no.esa.battleship.utils.flatMapIndexedNotNull
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
        val playerId = targeting.playerId
        val previousCoordinates = getPreviouslyAttemptedCoordinates(playerId)
        val availableCoordinates = getAvailableCoordinates(previousCoordinates)
        val intactShipTypes = getIntactShipTypes(targeting)
        val scoreMap = scoreCoordinatesForShipTypes(availableCoordinates, intactShipTypes)
        val scoringWasUnsuccessful = scoreMap.isEmpty() && availableCoordinates.isNotEmpty()

        return if (scoringWasUnsuccessful) {
            selectRandomCoordinateForPlayer(playerId, availableCoordinates)
        } else getHighestRankingCoordinate(scoreMap, previousCoordinates)
                ?: throw NoAvailableCoordinatesLeftException(this::class, ::seek, when {
                    intactShipTypes.isNotEmpty() && availableCoordinates.isEmpty() ->
                        "There are intact ships remaining, but no available coordinates!"
                    availableCoordinates.isEmpty() ->
                        "There are no available coordinates left. Game should've ended by now!"
                    scoreMap.isNotEmpty() ->
                        "Scoremap is not empty, but it still couldn't select a highest ranking coordinate!"
                    else ->
                        "Couldn't find coordinate due to an unknown cause!"
                })
    }

    /**
     * Attempts to specifically target an individual ship for destruction.
     *
     * This is done by finding previous strikes that hit a ship,
     * and firing at available coordinates adjacent with those that have been struck.
     *
     * todo
     *  when improving the targeting algorithm, start with how the coordinates
     *  are selected after a ship has been struck. It's currently a bit random,
     *  but can be made more targeted by calculating the axis of the ship.
     */
    private fun destroy(targeting: TargetingEntity): CoordinateEntity {
        val previousTurns = getPreviousTurnsForPlayer(targeting.playerId)
        val previouslyAttemptedCoordinates = getPreviouslyAttemptedCoordinates(previousTurns)
        val availableCoordinates = coordinateDao.getAll().filter { coordinateEntity ->
            coordinateEntity !in previouslyAttemptedCoordinates
        }

        val struckCoordinatesOnCurrentlyTargetedShips = findStruckCoordinatesOnCurrentlyTargetedShips(targeting,
                                                                                                      previouslyAttemptedCoordinates)
        val coordinatesAdjacentWithPreviousHits = findCoordinatesAdjacentWithPreviousHits(availableCoordinates,
                                                                                          struckCoordinatesOnCurrentlyTargetedShips)

        return coordinatesAdjacentWithPreviousHits.shuffled().firstOrNull()
                ?: throw NoValidCoordinatesException(this::class,
                                                     ::destroy,
                                                     "Found no suitable coordinates!")
    }

    fun scoreCoordinates(coordinates: List<CoordinateEntity>,
                         evaluationMode: CoordinateEvaluationMode): Map<CoordinateEntity, Int> {


        return scoreCoordinatesForShipTypes(coordinates, ShipType.values().toList())
    }

    private fun scoreCoordinatesForPlacement(coordinates: List<CoordinateEntity>,
                                             mode: CoordinateEvaluationMode): Map<CoordinateEntity, Int> {

        if (coordinates.isEmpty()) throw NoValidCoordinatesException(this::class,
                                                                     ::scoreCoordinatesForPlacement,
                                                                     "No coordinates to evaluate!")

        val scoreMap = scoreCoordinatesForShipTypes(coordinates, ShipType.values().toList())

        if (scoreMap.isEmpty()) throw NoValidCoordinatesException(this::class,
                                                                  ::scoreCoordinatesForPlacement,
                                                                  "No coordinates suitable for ship placement (HIGHLY doubt this)")

        return scoreMap
    }

    /**
     * This attempts to score every coordinate on the game board for every ship.
     *
     * This is done by counting the number of available configurations a ship can be placed
     * in across the game board, then returning a map of coordinates and their associated count (score).
     * The higher count, the more likely it is a ship is placed there.
     *
     * @param availableCoordinates are coordinates that have not yet been struck.
     * @param shipTypes are the still intact ship types.
     * @param axis is which axis to score for, i.e. vertical or horizontal.
     *
     * @return maps with coordinates and their count of possible ship configurations.
     */
    private fun scoreCoordinatesByShipTypes(availableCoordinates: List<CoordinateEntity>,
                                            shipTypes: List<ShipType>,
                                            axis: Axis): List<Map<CoordinateEntity, Int>> {

        if (availableCoordinates.isEmpty()) {
            throw NoValidCoordinatesException(this::class,
                                              ::scoreCoordinatesByShipTypes,
                                              "There are no coordinates to score!")
        }

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
                }.flatMapIndexedNotNull { index, _ ->
                    val coordinatesFromIndexCanFitShip = index + shipType.size < coordinates.size

                    if (coordinatesFromIndexCanFitShip) {
                        val rangeOfRequiredIndices = index..(index + shipType.size)

                        rangeOfRequiredIndices.map { requiredIndex ->
                            coordinates[requiredIndex]
                        }.takeIf(::coordinatesAreAdjacent)
                    } else null
                }
            }
        }.map { (_, coordinates) ->
            coordinates.groupingBy { coordinateEntity ->
                coordinateEntity
            }.eachCount()
        }
    }

    override fun getTargetCoordinate(targeting: TargetingEntity): CoordinateEntity {
        return if (targeting.targetingMode == SEEK) {
            seek(targeting)
        } else destroy(targeting)
    }

    private fun selectRandomCoordinateForPlayer(playerId: Int, availableCoordinates: List<CoordinateEntity>): CoordinateEntity {
        logger.warn("\t\t\tplayer $playerId - Selecting random coordinate.")

        return availableCoordinates.shuffled().first()
    }

    private fun getPreviouslyAttemptedCoordinates(playerId: Int): List<CoordinateEntity> {
        return turnDao.getPreviousTurnsByPlayerId(playerId).map {
            it.coordinateEntity
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

    private fun findStruckCoordinatesOnCurrentlyTargetedShips(targeting: TargetingEntity,
                                                              previouslyAttemptedCoordinates: List<CoordinateEntity>): List<CoordinateEntity> {

        return findCurrentlyTargetedShipsWithComponents(targeting).filter { ship ->
            ship.components.any {
                it.coordinateEntity in previouslyAttemptedCoordinates
            }
        }.flatMap { ship ->
            ship.components
                    .filter { it.isDestroyed }
                    .map { it.coordinateEntity }
        }
    }

    private fun findCoordinatesAdjacentWithPreviousHits(availableCoordinates: List<CoordinateEntity>,
                                                        struckCoordinatesOnCurrentlyTargetedShips: List<CoordinateEntity>): List<CoordinateEntity> {
        return availableCoordinates.filter { availableCoordinate ->
            struckCoordinatesOnCurrentlyTargetedShips.any { struckCoordinate ->
                struckCoordinate isAdjacentWith availableCoordinate
            }
        }
    }

    private fun getPreviousTurnsForPlayer(playerId: Int): List<TurnEntity> {
        return turnDao.getPreviousTurnsByPlayerId(playerId).sortedBy { it.gameTurn }
    }

    private fun getPreviouslyAttemptedCoordinates(previousTurns: List<TurnEntity>): List<CoordinateEntity> {
        return previousTurns.map { it.coordinateEntity }
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

    private fun getAvailableCoordinates(previousCoordinates: List<CoordinateEntity>): List<CoordinateEntity> {
        return coordinateDao.getAll().filter { coordinate ->
            coordinate !in previousCoordinates
        }
    }

    private fun scoreCoordinatesForShipTypes(availableCoordinates: List<CoordinateEntity>,
                                             shipTypes: List<ShipType>): Map<CoordinateEntity, Int> {

        val verticalScoreMaps = scoreCoordinatesByShipTypes(availableCoordinates, shipTypes, VERTICAL)
        val verticalScoreMap = combineScoreMaps(verticalScoreMaps)

        val horizontalScoreMaps = scoreCoordinatesByShipTypes(availableCoordinates, shipTypes, HORIZONTAL)
        val horizontalScoreMap = combineScoreMaps(horizontalScoreMaps)

        val scoreMaps = listOf(verticalScoreMap,
                               horizontalScoreMap)

        return combineScoreMaps(scoreMaps)
    }

    private fun combineScoreMaps(scoreMaps: List<Map<CoordinateEntity, Int>>): Map<CoordinateEntity, Int> {
        return scoreMaps.fold(mutableMapOf<CoordinateEntity, Int>()) { map, otherMap ->

            otherMap.forEach { (coordinate, score) ->
                map.merge(coordinate, score, Integer::sum)
            }

            map
        }.toMap()
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
        }.validateElements { current, next ->
            current isAdjacentWith next
        }
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
