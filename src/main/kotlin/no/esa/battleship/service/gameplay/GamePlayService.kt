package no.esa.battleship.service.gameplay

import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.InvalidPerformanceException
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playershipstatus.IPlayerShipStatusDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playertargeting.IPlayerTargetingDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.service.domain.*
import no.esa.battleship.service.targeting.ITargetingService
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val playerDao: IPlayerDao,
                      private val gameDao: IGameDao,
                      private val playerShipDao: IPlayerShipDao,
                      private val playerShipComponentDao: IPlayerShipComponentDao,
                      private val playerShipStatusDao: IPlayerShipStatusDao,
                      private val playerTurnDao: IPlayerTurnDao,
                      private val playerTargetingDao: IPlayerTargetingDao,
                      private val playerStrategyDao: IPlayerStrategyDao,
                      private val resultDao: IResultDao,
                      private val targetingService: ITargetingService) : IGamePlayService {

    override fun playGame(gameId: Int): GameReport {
        val game = gameDao.get(gameId)
        val (player1, player2) = getPlayersInGame(gameId)

        var gameTurnId = 1

        playerTargetingDao.save(player1.id, player2.id, gameTurnId)
        playerTargetingDao.save(player2.id, player1.id, gameTurnId)

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

        val (targeting, targetedShips) = targetingService.getPlayerTargeting(currentPlayer.id)
        val shipComponentsForCurrentPlayer = getShipComponentsForPlayer(currentPlayer.id)
        val shipComponentsForTargetPlayer = getShipComponentsForPlayer(targetPlayer.id)

        if (shipComponentsForCurrentPlayer.any { !it.isDestroyed }) {
            val targetCoordinate = targetingService.getTargetCoordinate(targeting, targetedShips)

            shipComponentsForTargetPlayer.firstOrNull { shipComponent ->
                shipComponent.coordinate == targetCoordinate
            }.let { shipComponent ->
                if (shipComponent != null) {
                    if (targeting.targetingMode != DESTROY) {
                        playerTargetingDao.update(currentPlayer.id, DESTROY)
                    }

                    playerShipComponentDao.update(shipComponent.id, true)
                    val allConnectedComponentsAreDestroyed = allConnectedComponentsAreDestroyed(shipComponentsForTargetPlayer,
                                                                                                shipComponent,
                                                                                                targetedShips)

                    if (allConnectedComponentsAreDestroyed) {
                        playerShipStatusDao.update(shipComponent.playerShipId, DESTROYED)
                    }

                    val allTargetedShipsAreDestroyed = targetedShips.flatMap {
                        playerShipComponentDao.findByPlayerShipId(it.playerShipId)
                    }.all { it.isDestroyed }

                    if (allTargetedShipsAreDestroyed) {
                        playerTargetingDao.update(targeting.playerId, SEEK)
                    }
                }

                playerTurnDao.save(currentPlayer.id,
                                   targetPlayer.id,
                                   targetCoordinate.id,
                                   shipComponent != null,
                                   gameTurn)
            }

        } else gameDao.conclude(currentPlayer.gameId)
    }

    /**
     * Checks if targeted connecting ship components are destroyed.
     *
     * @param componentsForTargetPlayer contains the components to check.
     * @param component is the component that will not be checked (is not updated yet).
     * @param targetedShips is used to filter the list to the ones currently targeted.
     *
     * @return true if every ship is destroyed, otherwise false.
     */
    private fun allConnectedComponentsAreDestroyed(componentsForTargetPlayer: List<Component>,
                                                   component: Component,
                                                   targetedShips: List<PlayerTargetedShip>): Boolean {
        return componentsForTargetPlayer
                .filterNot { it.id == component.id }
                .filter {
                    it.playerShipId in targetedShips.map { targetedShip ->
                        targetedShip.playerShipId
                    }
                }.all { it.isDestroyed }
    }

    private fun getShipComponentsForPlayer(playerId: Int): List<Component> {
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
