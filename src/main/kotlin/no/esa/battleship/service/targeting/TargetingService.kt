package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.enums.TargetingMode.SEEK
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
     * Selects a random coordinate based on the available coordinates a player has left.
     *
     * This should ideally rarely be done, unless several coordinates are scored identically.
     */
    private fun selectRandomCoordinate(playerId: Int, coordinates: List<CoordinateEntity>): CoordinateEntity {
        if (coordinates.isEmpty()) throw NoValidCoordinatesException(this::class,
                                                                     ::selectRandomCoordinate,
                                                                     "No available coordinates left!")

        logger.warn("\t\t\tplayer $playerId - Selecting random coordinate.")

        return coordinates.shuffled().first()
    }

    /**
     * Targeting follows a seek or destroy principle.
     *
     * Until a ship has been struck, the targeting attempts to calculate which
     * coordinates are the most likely to contain a ship. It does so by
     * counting the number of different ship configurations a coordinate can
     * contain, and then picking the highest scoring coordinate if possible.
     */
    private fun seek(targeting: TargetingEntity): CoordinateEntity {
        val previousCoordinates = turnDao.getPreviousTurnsForPlayer(targeting.playerId).map {
            it.coordinateEntity
        }
        val availableCoordinates = getAvailableCoordinates(previousCoordinates)
        val intactShipTypes = getIntactShipTypes(targeting)
        val scoreMap = scoreCoordinatesForShipTypes(availableCoordinates, intactShipTypes)

        return if (scoreMap.isEmpty() && availableCoordinates.isNotEmpty()) {
            selectRandomCoordinate(targeting.playerId, availableCoordinates)
        } else scoreMap.filterKeys { coordinateEntity ->
            coordinateEntity !in previousCoordinates
        }.maxBy { (_, score) ->
            score
        }?.key ?: throw NoValidCoordinatesException(this::class, ::seek, when {
            availableCoordinates.isEmpty() && intactShipTypes.isNotEmpty() ->
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
     * Attempts to finish off a partially destroyed ship by firing at available coordinates adjacent with previous hits,
     * sorted by game turn. There's room for improvements in this elaborated in the comment inside the function body.
     */
    private fun destroy(targeting: TargetingEntity): CoordinateEntity {
        val allCoordinates = coordinateDao.findAll()
        val previousTurns = turnDao.getPreviousTurnsForPlayer(targeting.playerId).sortedBy { it.gameTurn }
        val previouslyAttemptedCoordinates = previousTurns.map { it.coordinateEntity }
        val availableCoordinates = allCoordinates.filter { it !in previouslyAttemptedCoordinates }
        val struckCoordinatesOnCurrentlyTargetedShips = findStruckCoordinatesOnCurrentlyTargetedShip(targeting,
                                                                                                     previouslyAttemptedCoordinates)
        val coordinatesAdjacentWithPreviousHits = findAvailableCoordinatesAdjacentWithPreviousHits(availableCoordinates,
                                                                                                   struckCoordinatesOnCurrentlyTargetedShips)

        return coordinatesAdjacentWithPreviousHits
                .shuffled()
                /*
                todo
                 when a more sophisticated targeting algorithm should be implemented,
                 start with this. Instead of randomly picking an available adjacent
                 coordinate, it should try to align coordinates on the same axis
                 as previous ones (because a ship is coordinates in a straight line).
                */
                .firstOrNull()
                ?: throw NoValidCoordinatesException(this::class,
                                                     ::destroy,
                                                     "Found no suitable coordinates!")
    }

    private fun findAvailableCoordinatesAdjacentWithPreviousHits(availableCoordinates: List<CoordinateEntity>,
                                                                 struckCoordinatesOnCurrentlyTargetedShips: List<CoordinateEntity>): List<CoordinateEntity> {
        return availableCoordinates.filter { availableCoordinate ->
            struckCoordinatesOnCurrentlyTargetedShips.any { struckCoordinate ->
                struckCoordinate isAdjacentWith availableCoordinate
            }
        }
    }

    private fun findStruckCoordinatesOnCurrentlyTargetedShip(targeting: TargetingEntity,
                                                             previouslyAttemptedCoordinates: List<CoordinateEntity>): List<CoordinateEntity> {
        return findCurrentlyTargetedShipsWithComponents(targeting).filter { ship ->
            ship.components.any { componentEntity ->
                componentEntity.coordinateEntity in previouslyAttemptedCoordinates
            }
        }.flatMap { ship ->
            ship.components
                    .filter { it.isDestroyed }
                    .map { it.coordinateEntity }
        }
    }

    private fun findCurrentlyTargetedShipsWithComponents(targeting: TargetingEntity): List<ShipWithComponents> {
        return targetedShipDao.findByTargetingId(targeting.id).map { targetedShipEntity ->
            val shipEntity = shipDao.find(targetedShipEntity.shipId)
            val componentEntities = componentDao.findByPlayerShipId(shipEntity.id)
            val shipType = ShipType.fromInt(shipEntity.shipTypeId)
            val components = Components(shipType, componentEntities)

            ShipWithComponents(shipEntity, components)
        }
    }

    private fun getIntactShipTypes(targeting: TargetingEntity): List<ShipType> {
        return shipStatusDao.findAll(targeting.targetPlayerId).filterValues { shipStatus ->
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

    private fun getAvailableCoordinates(previousCoordinates: List<CoordinateEntity>): List<CoordinateEntity> {

        return coordinateDao.findAll().filter { coordinate ->
            coordinate !in previousCoordinates
        }
    }

    /**
     * Scores coordinates by counting the amount of ship configurations
     * every single coordinate can hold.
     *
     * Iterates through all the shipTypes passed as an argument, and builds a map
     * containing a coordinate and its associated score. This map is then
     * used to select a coordinate later on based on its score (higher score means
     * it can be used for more different ship placements which means it's more likely
     * to contain a ship than a coordinate with a lower score).
     */
    private fun scoreCoordinates(availableCoordinates: List<CoordinateEntity>,
                                 shipTypes: List<ShipType>,
                                 axis: Axis): Map<ShipType, Map<CoordinateEntity, Int>> {

        if (availableCoordinates.isEmpty()) throw RuntimeException("There are no coordinates to score!")

        return shipTypes.map { shipType ->
            shipType to availableCoordinates.groupBy { coordinateEntity ->
                when (axis) {
                    VERTICAL -> coordinateEntity.vertical_position
                    HORIZONTAL -> coordinateEntity.horizontalPositionAsInt()
                }
            }.flatMap { (_, coordinates) ->
                coordinates.sortedBy { coordinateEntity ->
                    when (axis) {
                        VERTICAL -> coordinateEntity.vertical_position
                        HORIZONTAL -> coordinateEntity.horizontalPositionAsInt()
                    }
                }.mapIndexedNotNull { index, _ ->
                    if (index + shipType.size < coordinates.size) {
                        (index..(index + shipType.size)).map {
                            coordinates[it]
                        }.takeIf { coordinatesAreAdjacent(it) }
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
                VERTICAL -> coordinate.vertical_position
            }
        }.validateElements { current, next -> current isAdjacentWith next }
    }

    override fun getTargeting(playerId: Int): TargetingEntity {
        return targetingDao.find(playerId)
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
        return targetedShipDao.findByTargetingId(targetingId)
    }
}
