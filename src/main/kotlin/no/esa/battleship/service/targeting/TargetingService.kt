package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playershipstatus.IPlayerShipStatusDao
import no.esa.battleship.repository.playertargetedship.IPlayerTargetedShipDao
import no.esa.battleship.repository.playertargeting.IPlayerTargetingDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.PlayerTargetedShip
import no.esa.battleship.service.domain.PlayerTargeting
import no.esa.battleship.utils.isAdjacentWith
import org.springframework.stereotype.Service

@Service
class TargetingService(private val playerShipComponentDao: IPlayerShipComponentDao,
                       private val playerShipStatusDao: IPlayerShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val playerTargetingDao: IPlayerTargetingDao,
                       private val playerTargetedShipDao: IPlayerTargetedShipDao,
                       private val playerTurnDao: IPlayerTurnDao) : ITargetingService {

    override fun getPlayerTargeting(playerId: Int): Pair<PlayerTargeting, List<PlayerTargetedShip>> {
        return playerTargetingDao.find(playerId).let {
            val ships = playerTargetedShipDao.findByTargetingId(it.id)

            it to ships
        }
    }

    override fun getTargetCoordinate(targeting: PlayerTargeting,
                                     targetedShips: List<PlayerTargetedShip>): Coordinate {

        val unavailableCoordinates = getUnavailableCoordinates(targeting.playerId, targeting.targetPlayerId)
        val availableCoordinates = coordinateDao.findAll().filterNot { it in unavailableCoordinates }
        val remainingEnemyShips = playerShipStatusDao.findAll(targeting.targetPlayerId)
                .filterValues { it != DESTROYED }
                .keys
                .toList()

        return when (targeting.targetingMode) {
            SEEK -> {
                val scoreMap = remainingEnemyShips.map { ship ->
                    scoreCoordinates(availableCoordinates, ShipType.fromInt(ship.shipTypeId))
                }.fold(mutableMapOf<Coordinate, Int>()) { acc, map ->
                    map.entries.forEach { (coordinate, score) ->
                        acc.merge(coordinate, score, Integer::sum)
                    }
                    acc
                }.toMap()

                scoreMap.entries.maxBy { (_, score) -> score }?.key ?: availableCoordinates.random()
            }
            DESTROY -> {
                val relevantMoves = playerTurnDao.getPreviousTurnsForPlayer(targeting.playerId)
                val relevantHits = relevantMoves.filter { it.isHit }
                val relevantCoordinates = availableCoordinates.flatMap { coordinate ->
                    relevantHits.filter { turn ->
                        coordinate isAdjacentWith turn.coordinate
                    }.map { it.coordinate }
                }

                val scoreMap = remainingEnemyShips.map {
                    scoreCoordinates(relevantCoordinates, ShipType.fromInt(it.shipTypeId))
                }.fold(mutableMapOf<Coordinate, Int>()) { acc, map ->
                    map.entries.forEach { (coordinate, score) ->
                        acc.merge(coordinate, score, Integer::sum)
                    }
                    acc
                }.toMap()

                scoreMap.entries.maxBy { (_, score) -> score }?.key ?: availableCoordinates.random()
            }
        }
    }

    private fun getUnavailableCoordinates(playerId: Int, targetPlayerId: Int): List<Coordinate> {
        val destroyedComponentCoordinates = playerShipStatusDao.findAll(targetPlayerId).filterValues { status ->
            status == DESTROYED
        }.map { (ship, _) ->
            ship.id
        }.flatMap { shipId ->
            playerShipComponentDao.findByPlayerShipId(shipId).map { component ->
                component.coordinate
            }
        }

        val previousCoordinates = playerTurnDao.getPreviousTurnsForPlayer(playerId).map { turn ->
            turn.coordinate
        }

        return listOf(destroyedComponentCoordinates, previousCoordinates)
                .flatten()
                .distinct()
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
                coordinate.vertical_position
            } else coordinate.horizontalPositionAsInt()
        }.flatMap { (_, coordinates: List<Coordinate>) ->
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

            minimumRange.mapNotNull { shipComponentCoordinate ->
                coordinates.getOrNull(shipComponentCoordinate)
            }.takeIf { shipComponentCoordinates ->
                shipComponentCoordinates.size == shipType.size
            }
        }
    }
}
