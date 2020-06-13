package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.Axis.HORIZONTAL
import no.esa.battleship.enums.Axis.VERTICAL
import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.enums.TargetingMode.DESTROY
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
import no.esa.battleship.utils.isAdjacentWith
import no.esa.battleship.utils.validateElements
import org.springframework.stereotype.Service

@Service
class TargetingService(private val componentDao: IComponentDao,
                       private val shipStatusDao: IShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val shipDao: IShipDao,
                       private val targetingDao: ITargetingDao,
                       private val targetedShipDao: ITargetedShipDao,
                       private val turnDao: ITurnDao) : ITargetingService {

    override fun getTargetCoordinate(targeting: TargetingEntity): CoordinateEntity {
        return when (targeting.targetingMode) {
            SEEK -> seek(targeting)
            DESTROY -> {
                destroy(targeting)
            }
        }
    }

    private fun seek(targeting: TargetingEntity): CoordinateEntity {
        val availableCoordinates = getAvailableCoordinates(targeting)
        val previousCoordinates = turnDao.getPreviousTurnsForPlayer(targeting.playerId).map {
            it.coordinateEntity
        }
        val intactShipTypes = getIntactShipTypes(targeting)

        val rankedCoordinatesByShipType = scoreAvailableCoordinatesForShipTypes(availableCoordinates, intactShipTypes)

        val scoreMaps = rankedCoordinatesByShipType.filterKeys { shipType ->
            shipType in intactShipTypes
        }.map { (_, scoreMap) ->
            scoreMap
        }

        return combineScoreMaps(scoreMaps).filterKeys {
            it !in previousCoordinates
        }.maxBy { (_, score) ->
            score
        }?.key ?: throw NoValidCoordinatesException("Found no suitable coordinates!")
    }

    private fun destroy(targeting: TargetingEntity): CoordinateEntity {
        val targetedShips = targetedShipDao.findByTargetingId(targeting.id)
        val remainingShipTypes = shipStatusDao.findAll(targeting.targetPlayerId)
                .filter { (_, status) ->
                    status == ShipStatus.INTACT
                }.map { (shipEntity, _) ->
                    ShipType.fromInt(shipEntity.shipTypeId)
                }
        val allCoordinates = coordinateDao.findAll()
        val previousTurns = turnDao.getPreviousTurnsForPlayer(targeting.playerId).sortedBy { it.gameTurn }
        val previouslyStruckCoordinates = previousTurns.map { turn ->
            turn.coordinateEntity
        }

        // Find the coordinates that led to each discovery of a ship.
        // This is to be able to find their adjacent coordinates,
        // and to limit the knowledge of enemy ships to confirmed hits.
        val initiallyHitCoordinatesForTargetedShips = targetedShips.map {
            val shipEntity = shipDao.find(it.shipId)
            componentDao.findByPlayerShipId(shipEntity.id)
        }.mapNotNull { components ->
            components.firstOrNull { componentEntity ->
                componentEntity.coordinateEntity in previouslyStruckCoordinates
            }
        }.map { componentEntity ->
            componentEntity.coordinateEntity
        }

        val coordinatesAdjacentWithPreviousHits = allCoordinates.filter { coordinateEntity ->
            coordinateEntity !in previouslyStruckCoordinates
        }.filter { coordinateEntity ->
            initiallyHitCoordinatesForTargetedShips.any { struckCoordinateEntity ->
                coordinateEntity isAdjacentWith struckCoordinateEntity
            }
        }

        val scoreMapsByShipRemainingTypes = scoreAvailableCoordinatesForShipTypes(coordinatesAdjacentWithPreviousHits,
                                                                                  remainingShipTypes)

        return scoreMapsByShipRemainingTypes.map { (_, scoreMaps) ->
            scoreMaps
        }.let { scoreMaps ->
            combineScoreMaps(scoreMaps)
        }.maxBy { (_, score) ->
            score
        }?.key ?: throw NoValidCoordinatesException("Found no suitable coordinates!")
    }

    private fun findDestroyedCoordinates(targeting: TargetingEntity): List<CoordinateEntity> {
        return targetedShipDao.findByTargetingId(targeting.id).flatMap { targetedShip ->
            componentDao.findByPlayerShipId(targetedShip.shipId)
        }.filter { component ->
            component.isDestroyed
        }.map { component ->
            component.coordinateEntity
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

    private fun getAvailableCoordinates(targeting: TargetingEntity): List<CoordinateEntity> {
        val previousCoordinates = turnDao.getPreviousTurnsForPlayer(targeting.playerId).map {
            it.coordinateEntity
        }

        return coordinateDao.findAll().filter { coordinate ->
            coordinate !in previousCoordinates
        }
    }

    private fun scoreAvailableCoordinatesForShipTypes(availableCoordinates: List<CoordinateEntity>,
                                                      shipTypes: List<ShipType>): Map<ShipType, Map<CoordinateEntity, Int>> {
        return shipTypes.map { shipType ->
            shipType to availableCoordinates.groupBy {
                it.horizontalPositionAsInt()
            }.mapNotNull { (_, coordinates) ->
                coordinates.sortedBy { coordinate ->
                    coordinate.vertical_position
                }.mapIndexedNotNull { index, _ ->
                    if (index + shipType.size < coordinates.size) {
                        (index..(index + shipType.size)).map {
                            coordinates[it]
                        }.takeIf {
                            coordinatesAreAdjacent(it)
                        }
                    } else null
                }.flatten()
            }.flatten()
        }.toMap().mapValues { (_, coordinates) ->
            coordinates.groupingBy {
                it
            }.eachCount()
        }
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
        }.validateElements { current, next ->
            current isAdjacentWith next
        }
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
