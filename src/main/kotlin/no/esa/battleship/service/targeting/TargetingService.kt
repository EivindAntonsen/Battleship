package no.esa.battleship.service.targeting

import no.esa.battleship.enums.Axis
import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipType
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playershipstatus.IPlayerShipStatusDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playertargetingmode.IPlayerTargetingModeDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.service.domain.Coordinate
import org.springframework.stereotype.Service

@Service
class TargetingService(private val playerShipComponentDao: IPlayerShipComponentDao,
                       private val playerShipStatusDao: IPlayerShipStatusDao,
                       private val coordinateDao: ICoordinateDao,
                       private val playerTurnDao: IPlayerTurnDao) : ITargetingService {

    override fun getTargetCoordinate(playerId: Int, targetPlayerId: Int): Coordinate {
        val unavailableCoordinates = getUnavailableCoordinates(playerId, targetPlayerId)
        val availableCoordinates = coordinateDao.findAll().filterNot { it in unavailableCoordinates }
        val remainingEnemyShips = playerShipStatusDao.findAll(targetPlayerId)
                .filterValues { it != DESTROYED }
                .keys
                .toList()

        val scoreMap = remainingEnemyShips.map { ship ->
            scoreCoordinates(availableCoordinates, ShipType.fromInt(ship.shipTypeId))
        }.fold(mutableMapOf<Coordinate, Int>()) { acc, map ->
            map.entries.forEach { (coordinate, _) ->
                acc.merge(coordinate, 1) { oldValue, newValue ->
                    oldValue + newValue
                }
            }

            acc
        }.toMap()

        println(scoreMap)

        TODO()
    }

    private fun getUnavailableCoordinates(playerId: Int, targetPlayerId: Int): List<Coordinate> {
        val destroyedShips = playerShipStatusDao.findAll(targetPlayerId).filterValues { status ->
            status == DESTROYED
        }.map { (ship, _) ->
            ship.id
        }
        val destroyedComponentCoordinates = destroyedShips.flatMap { shipId ->
            val componentCoordinates = playerShipComponentDao.findByPlayerShipId(shipId).map { component ->
                component.coordinate
            }

            componentCoordinates
        }

        val previousCoordinates = playerTurnDao.getPreviousTurnsForPlayer(playerId).map { turn ->
            turn.coordinate
        }

        return listOf(destroyedComponentCoordinates, previousCoordinates).flatten()
    }

    private fun scoreCoordinates(coordinates: List<Coordinate>, shipType: ShipType): Map<Coordinate, Int> {
        val scoreByHorizontalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.HORIZONTAL, coordinates)
        val scoreByVerticalGrouping = scoreCoordinatesByAxisForShipType(shipType, Axis.VERTICAL, coordinates)

        return combineScoreMaps(scoreByHorizontalGrouping, scoreByVerticalGrouping)
    }

    private fun combineScoreMaps(map1: Map<Coordinate, Int>,
                                 map2: Map<Coordinate, Int>): Map<Coordinate, Int> {

        return map1.entries.zip(map2.entries).fold(mutableMapOf<Coordinate, Int>()) { acc, entries ->
            acc.merge(entries.first.key, 1) { oldScore, newScore ->
                oldScore + newScore
            }

            acc.merge(entries.second.key, 1) { oldScore, newScore ->
                oldScore + newScore
            }

            acc
        }.toMap()
    }

    private fun scoreCoordinatesByAxisForShipType(shipType: ShipType,
                                                  axis: Axis,
                                                  coordinates: List<Coordinate>): Map<Coordinate, Int> {
        return coordinates.groupBy { coordinate ->
            if (axis == Axis.VERTICAL) {
                coordinate.vertical_position
            } else coordinate.horizontalPositionAsInt()
        }.flatMap { (_, coordinates) ->
            collectAvailableCoordinates(coordinates, shipType)
        }.fold(mutableMapOf<Coordinate, Int>()) { scoreMap, componentCoordinates ->
            componentCoordinates.forEach { coordinate ->
                scoreMap.merge(coordinate, 1) { oldScore, newScore ->
                    oldScore + newScore
                }
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
