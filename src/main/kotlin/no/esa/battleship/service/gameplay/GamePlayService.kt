package no.esa.battleship.service.gameplay

import no.esa.battleship.exceptions.InvalidPerformanceException
import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.service.domain.*
import no.esa.battleship.service.targeting.ITargetingService
import no.esa.battleship.service.targeting.TargetingService
import no.esa.battleship.utils.isAdjacentWith
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val coordinateDao: ICoordinateDao,
                      private val playerDao: IPlayerDao,
                      private val gameDao: IGameDao,
                      private val playerShipDao: IPlayerShipDao,
                      private val playerShipComponentDao: IPlayerShipComponentDao,
                      private val playerTurnDao: IPlayerTurnDao,
                      private val playerStrategyDao: IPlayerStrategyDao,
                      private val resultDao: IResultDao,
                      private val targetingService: ITargetingService) : IGamePlayService {

    override fun playGame(gameId: Int): GameReport {
        val game = gameDao.get(gameId)
        val (player1, player2) = getPlayersInGame(gameId)

        var gameTurnId = 1

        do {
            executeGameTurn(player1, player2, gameTurnId)
            executeGameTurn(player2, player1, gameTurnId)

            gameTurnId++
        } while (!gameDao.isGameConcluded(gameId))

        val remainingPlayers = findRemainingPlayers(gameId)

        val winningPlayer = when {
            remainingPlayers.size > 1 -> null
            remainingPlayers.isEmpty() -> null
            else -> remainingPlayers.first()
        }

        val playerInfos = listOf(
                PlayerInfo(player1,
                           playerStrategyDao.find(player1.id),
                           getPerformanceAnalysis(player1)),
                PlayerInfo(player2,
                           playerStrategyDao.find(player2.id),
                           getPerformanceAnalysis(player2)))

        return GameReport(game,
                          playerInfos,
                          resultDao.save(gameId, winningPlayer?.id))
    }

    private fun getPerformanceAnalysis(player: Player): PerformanceAnalysis {
        val previousTurns = playerTurnDao.getPreviousTurnsForPlayer(player.id)

        val hitCount = previousTurns.filter { it.isHit }.count()
        val missCount = previousTurns.filterNot { it.isHit }.count()
        val totalCount = hitCount + missCount

        if (totalCount == 0) throw InvalidPerformanceException(player.id)

        val hitRate = hitCount.toDouble() / totalCount.toDouble()

        return PerformanceAnalysis(player, totalCount, hitCount, missCount, hitRate)
    }

    override fun findRemainingPlayers(gameId: Int): List<Player> {
        return playerShipComponentDao.findByGameId(gameId).map { shipComponent ->
            val ship = playerShipDao.find(shipComponent.playerShipId)

            playerDao.find(ship.playerId)
        }.distinct()
    }

    private fun executeGameTurn(currentPlayer: Player,
                                targetPlayer: Player,
                                gameTurn: Int) {

        if (playerFleetIsAlive(currentPlayer.id)) {
            targetingService.getTargetCoordinate(currentPlayer.id, targetPlayer.id)

            val targetCoordinate = getTargetCoordinate(currentPlayer.id)

            getShipComponentsForPlayer(targetPlayer.id).firstOrNull { shipComponent ->
                shipComponent.coordinate == targetCoordinate
            }.let { shipComponent ->
                if (shipComponent != null) {
                    playerShipComponentDao.update(shipComponent.id, true)
                }

                playerTurnDao.save(currentPlayer.id,
                                   targetCoordinate.id,
                                   shipComponent != null,
                                   gameTurn)
            }

        } else gameDao.conclude(currentPlayer.gameId)
    }

    /**
     * Finds the next set of coordinates to target.
     *
     * It finds adjacent coordinates to the coordinates that have been
     * confirmed hits. If all adjacent coordinates have been exhausted, it
     * falls back to a random one.
     */
    override fun getTargetCoordinate(playerId: Int): Coordinate {
        val previousTurnsForPlayer = playerTurnDao.getPreviousTurnsForPlayer(playerId)
        val unavailableCoordinates = previousTurnsForPlayer.map { it.coordinate }.distinct()
        val availableCoordinates = coordinateDao.findAll().filter { coordinate ->
            coordinate !in unavailableCoordinates
        }

        val coordinatesAdjacentWithPreviousHits = previousTurnsForPlayer.filter { turn ->
            turn.isHit
        }.flatMap { previousHit ->
            availableCoordinates.filter { availableCoordinate ->
                previousHit.coordinate isAdjacentWith availableCoordinate
            }
        }

        return when {
            coordinatesAdjacentWithPreviousHits.isNotEmpty() -> coordinatesAdjacentWithPreviousHits
            availableCoordinates.isNotEmpty() -> availableCoordinates
            else -> throw NoValidCoordinatesException("No valid coordinates left for player $playerId!")
        }.random()
    }

    private fun playerFleetIsAlive(playerId: Int): Boolean {
        return getShipComponentsForPlayer(playerId).any { shipComponent ->
            !shipComponent.isDestroyed
        }
    }

    private fun getShipComponentsForPlayer(playerId: Int): List<ShipComponent> {
        return playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findByPlayerShipId(ship.id)
        }
    }

    private fun getPlayersInGame(gameId: Int): Pair<Player, Player> {
        return playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }
    }
}
