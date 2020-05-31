package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.mapper.CoordinateMapper
import no.esa.battleship.repository.mapper.TargetingMapper
import no.esa.battleship.repository.mapper.TurnMapper
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.targetedship.ITargetedShipDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Targeting
import no.esa.battleship.utils.isAdjacentWith
import org.springframework.stereotype.Service

@Service
class TargetingService(private val componentDao: IComponentDao,
                       private val shipStatusDao: IShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val targetingDao: ITargetingDao,
                       private val targetedShipDao: ITargetedShipDao,
                       private val turnDao: ITurnDao) : ITargetingService {

    override fun getTargeting(playerId: Int): Targeting {
        val targetingEntity = targetingDao.find(playerId)
        val targetedShipEntities = targetedShipDao.findByTargetingId(targetingEntity.id)

        return TargetingMapper.toTargeting(targetingEntity, targetedShipEntities)
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

    override fun getTargetCoordinate(targeting: Targeting): Coordinate {
        val allCoordinates = coordinateDao.findAll().map(CoordinateMapper::toCoordinate)
        val unavailableCoordinates = getUnavailableCoordinates(targeting)
        val availableCoordinates = allCoordinates.filterNot { it in unavailableCoordinates }

        val remainingEnemyShipTypes = shipStatusDao.findAll(targeting.targetPlayerId)
                .filterValues { it != DESTROYED }
                .keys
                .toList()
                .map { ShipType.fromInt(it.shipTypeId) }

        val scoreMap = scoreCoordinatesForShipTypes(remainingEnemyShipTypes, availableCoordinates)

        return when (targeting.targetingMode) {
            SEEK -> scoreMap
            DESTROY -> {
                val relevantMoves = turnDao.getPreviousTurnsForPlayer(targeting.playerId).map(TurnMapper::toTurn)
                val relevantHits = relevantMoves.filter { it.isHit }
                val relevantCoordinates = availableCoordinates.flatMap { coordinate ->
                    relevantHits.filter { turn ->
                        coordinate isAdjacentWith turn.coordinate
                    }.map { it.coordinate }
                }

                scoreCoordinatesForShipTypes(remainingEnemyShipTypes, relevantCoordinates)
            }
        }.entries.maxBy { (_, score) -> score }?.key
                ?: availableCoordinates.shuffled().firstOrNull()
                ?: throw NoValidCoordinatesException("No available coordinates left! ${unavailableCoordinates.size} unavailable coordinates registered.")
    }

    private fun scoreCoordinatesForShipTypes(remainingEnemyShipTypes: List<ShipType>,
                                             coordinates: List<Coordinate>): Map<Coordinate, Int> {
        return remainingEnemyShipTypes.map { shipType ->
            scoreCoordinates(coordinates, shipType)
        }.fold(mutableMapOf<Coordinate, Int>()) { acc, map ->
            map.entries.forEach { (coordinate, score) ->
                acc.merge(coordinate, score, Integer::sum)
            }

            acc
        }.toMap()
    }

    private fun getUnavailableCoordinates(targeting: Targeting): List<Coordinate> {
        val previousTurnsForPlayer = turnDao.getPreviousTurnsForPlayer(targeting.playerId)
        val destroyedComponentCoordinates = shipStatusDao.findAll(targeting.targetPlayerId)
                .filterValues { status ->
                    status == DESTROYED
                }.map { (ship, _) ->
                    ship.id
                }.flatMap { shipId ->
                    componentDao.findByPlayerShipId(shipId).map { component ->
                        component.coordinateEntity
                    }
                }

        val previousCoordinates = previousTurnsForPlayer.map { turn ->
            turn.coordinateEntity
        }

        return listOf(destroyedComponentCoordinates, previousCoordinates)
                .flatten()
                .distinct()
                .map(CoordinateMapper::toCoordinate)
    }

    private fun scoreCoordinates(coordinates: List<Coordinate>, shipType: ShipType): Map<Coordinate, Int> {
        val scoreByHorizontalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.HORIZONTAL, coordinates)
        val scoreByVerticalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.VERTICAL, coordinates)

        return combineScoreMaps(scoreByHorizontalGrouping, scoreByVerticalGrouping)
    }

    private fun combineScoreMaps(map1: Map<Coordinate, Int>,
                                 map2: Map<Coordinate, Int>): Map<Coordinate, Int> {
        return mutableMapOf<Coordinate, Int>().apply {
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
                                                  coordinates: List<Coordinate>): Map<Coordinate, Int> {
        return coordinates.groupBy { coordinate ->
            if (axis == Axis.VERTICAL) {
                coordinate.y
            } else coordinate.xAsInt()
        }.flatMap { (_, coordinates) ->
            collectAvailableCoordinates(coordinates, shipType)
        }.fold(mutableMapOf<Coordinate, Int>()) { scoreMap, componentCoordinates ->
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
    private fun collectAvailableCoordinates(coordinates: List<Coordinate>, shipType: ShipType): List<List<Coordinate>> {
        return coordinates.mapIndexedNotNull { index, _ ->
            val minimumRange = index..(index + shipType.size)

            minimumRange.mapNotNull { currentIndex ->
                coordinates.getOrNull(currentIndex)
            }.takeIf { coordinates ->
                coordinates.size == shipType.size
            }
        }
    }
}
