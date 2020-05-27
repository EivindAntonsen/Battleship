package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.targetedship.ITargetedShipDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.entity.TargetingEntity
import no.esa.battleship.utils.isAdjacentWith
import org.springframework.stereotype.Service

@Service
class TargetingService(private val componentDao: IComponentDao,
                       private val shipStatusDao: IShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val targetingDao: ITargetingDao,
                       private val targetedShipDao: ITargetedShipDao,
                       private val turnDao: ITurnDao) : ITargetingService {

    override fun getPlayerTargeting(playerId: Int): Pair<TargetingEntity, List<TargetedShipEntity>> {
        return targetingDao.find(playerId).let {
            val ships = targetedShipDao.findByTargetingId(it.id)

            it to ships
        }
    }

    override fun getTargetCoordinate(targetingEntity: TargetingEntity,
                                     targetedShipEntities: List<TargetedShipEntity>): CoordinateEntity {

        val unavailableCoordinates = getUnavailableCoordinates(targetingEntity.playerId, targetingEntity.targetPlayerId)
        val availableCoordinates = coordinateDao.findAll().filterNot { it in unavailableCoordinates }
        val remainingEnemyShips = shipStatusDao.findAll(targetingEntity.targetPlayerId)
                .filterValues { it != DESTROYED }
                .keys
                .toList()

        return when (targetingEntity.targetingMode) {
            SEEK -> {
                val scoreMap = remainingEnemyShips.map { ship ->
                    scoreCoordinates(availableCoordinates, ShipType.fromInt(ship.shipTypeId))
                }.fold(mutableMapOf<CoordinateEntity, Int>()) { acc, map ->
                    map.entries.forEach { (coordinate, score) ->
                        acc.merge(coordinate, score, Integer::sum)
                    }
                    acc
                }.toMap()

                scoreMap
            }
            DESTROY -> {
                val relevantMoves = turnDao.getPreviousTurnsForPlayer(targetingEntity.playerId)
                val relevantHits = relevantMoves.filter { it.isHit }
                val relevantCoordinates = availableCoordinates.flatMap { coordinate ->
                    relevantHits.filter { turn ->
                        coordinate isAdjacentWith turn.coordinateEntity
                    }.map { it.coordinateEntity }
                }

                val scoreMap = remainingEnemyShips.map {
                    scoreCoordinates(relevantCoordinates, ShipType.fromInt(it.shipTypeId))
                }.fold(mutableMapOf<CoordinateEntity, Int>()) { acc, map ->
                    map.entries.forEach { (coordinate, score) ->
                        acc.merge(coordinate, score, Integer::sum)
                    }
                    acc
                }.toMap()

                scoreMap
            }
        }.entries.maxBy { (_, score) -> score }?.key
                ?: availableCoordinates.shuffled().firstOrNull()
                ?: throw NoValidCoordinatesException("No available coordinates left! ${unavailableCoordinates.size} unavailable coordinates registered.")
    }

    private fun getUnavailableCoordinates(playerId: Int, targetPlayerId: Int): List<CoordinateEntity> {
        val destroyedComponentCoordinates = shipStatusDao.findAll(targetPlayerId).filterValues { status ->
            status == DESTROYED
        }.map { (ship, _) ->
            ship.id
        }.flatMap { shipId ->
            componentDao.findByPlayerShipId(shipId).map { component ->
                component.coordinateEntity
            }
        }

        val previousCoordinates = turnDao.getPreviousTurnsForPlayer(playerId).map { turn ->
            turn.coordinateEntity
        }

        return listOf(destroyedComponentCoordinates, previousCoordinates)
                .flatten()
                .distinct()
    }

    private fun scoreCoordinates(coordinateEntities: List<CoordinateEntity>, shipType: ShipType): Map<CoordinateEntity, Int> {
        val scoreByHorizontalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.HORIZONTAL, coordinateEntities)
        val scoreByVerticalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.VERTICAL, coordinateEntities)

        return combineScoreMaps(scoreByHorizontalGrouping, scoreByVerticalGrouping)
    }

    private fun combineScoreMaps(map1: Map<CoordinateEntity, Int>,
                                 map2: Map<CoordinateEntity, Int>): Map<CoordinateEntity, Int> {

        return mutableMapOf<CoordinateEntity, Int>().apply {
            map1.entries.forEach { (coordinate, score) ->
                merge(coordinate, score) { oldScore, newScore ->
                    oldScore + newScore
                }
            }

            map2.entries.forEach { (coordinate, score) ->
                merge(coordinate, score) { oldScore, newScore ->
                    oldScore + newScore
                }
            }
        }.toMap()
    }

    private fun scoreCoordinatesByAxisForShipType(shipType: ShipType,
                                                  axis: Axis,
                                                  coordinateEntities: List<CoordinateEntity>): Map<CoordinateEntity, Int> {
        return coordinateEntities.groupBy { coordinate ->
            if (axis == Axis.VERTICAL) {
                coordinate.vertical_position
            } else coordinate.horizontalPositionAsInt()
        }.flatMap { (_, coordinateEntities: List<CoordinateEntity>) ->
            collectAvailableCoordinates(coordinateEntities, shipType)
        }.fold(mutableMapOf<CoordinateEntity, Int>()) { scoreMap, componentCoordinates ->
            componentCoordinates.forEach { coordinate ->
                scoreMap.merge(coordinate, 1, Integer::sum)
            }

            scoreMap
        }.toMap()
    }

    /**
     * Filters out any index position that would have gaps / holes in the list of coordinates.
     *
     * This is done to ensure no coordinates are submitted where a ship wouldn't be
     * able to be positioned.
     *
     * @return a list of lists containing available coordinates for ship components for the given
     *         ship type.
     */
    private fun collectAvailableCoordinates(coordinateEntities: List<CoordinateEntity>, shipType: ShipType): List<List<CoordinateEntity>> {
        return coordinateEntities.mapIndexedNotNull { index, _ ->
            val minimumRange = index..(index + shipType.size)

            minimumRange.mapNotNull { shipComponentCoordinate ->
                coordinateEntities.getOrNull(shipComponentCoordinate)
            }.takeIf { shipComponentCoordinates ->
                shipComponentCoordinates.size == shipType.size
            }
        }
    }
}
