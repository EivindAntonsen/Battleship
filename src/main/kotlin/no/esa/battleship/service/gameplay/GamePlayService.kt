package no.esa.battleship.service.gameplay

import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.InvalidPerformanceException
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.service.domain.*
import no.esa.battleship.repository.entity.ComponentEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.service.targeting.ITargetingService
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val playerDao: IPlayerDao,
                      private val gameDao: IGameDao,
                      private val shipDao: IShipDao,
                      private val componentDao: IComponentDao,
                      private val shipStatusDao: IShipStatusDao,
                      private val turnDao: ITurnDao,
                      private val targetingDao: ITargetingDao,
                      private val playerStrategyDao: IPlayerStrategyDao,
                      private val resultDao: IResultDao,
                      private val targetingService: ITargetingService) : IGamePlayService {

    override fun playGame(gameId: Int): GameReport {
        val game = gameDao.get(gameId)
        val (player1, player2) = getPlayersInGame(gameId)

        var gameTurnId = 1

        targetingDao.save(player1.id, player2.id, gameTurnId)
        targetingDao.save(player2.id, player1.id, gameTurnId)

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

    private fun getPerformanceAnalysis(playerEntity: PlayerEntity): PerformanceAnalysis {
        val previousTurns = turnDao.getPreviousTurnsForPlayer(playerEntity.id)

        val hitCount = previousTurns.filter { it.isHit }.count()
        val missCount = previousTurns.filterNot { it.isHit }.count()
        val totalCount = hitCount + missCount

        if (totalCount == 0) throw InvalidPerformanceException(playerEntity.id)

        val hitRate = hitCount.toDouble() / totalCount.toDouble()

        return PerformanceAnalysis(playerEntity, totalCount, hitCount, missCount, hitRate)
    }

    override fun findRemainingPlayers(gameId: Int): List<PlayerEntity> {
        return componentDao.findByGameId(gameId).map { shipComponent ->
            val ship = shipDao.find(shipComponent.shipId)

            playerDao.find(ship.playerId)
        }.distinct()
    }

    private fun executeGameTurn(currentPlayerEntity: PlayerEntity,
                                targetPlayerEntity: PlayerEntity,
                                gameTurn: Int) {

        val (targeting, targetedShips) = targetingService.getPlayerTargeting(currentPlayerEntity.id)
        val shipComponentsForCurrentPlayer = getShipComponentsForPlayer(currentPlayerEntity.id)
        val shipComponentsForTargetPlayer = getShipComponentsForPlayer(targetPlayerEntity.id)

        if (shipComponentsForCurrentPlayer.any { !it.isDestroyed }) {
            val targetCoordinate = targetingService.getTargetCoordinate(targeting, targetedShips)

            shipComponentsForTargetPlayer.firstOrNull { shipComponent ->
                shipComponent.coordinateEntity == targetCoordinate
            }.let { shipComponent ->
                if (shipComponent != null) {
                    if (targeting.targetingMode != DESTROY) {
                        targetingDao.update(currentPlayerEntity.id, DESTROY)
                    }

                    componentDao.update(shipComponent.id, true)
                    val allConnectedComponentsAreDestroyed = allConnectedComponentsAreDestroyed(shipComponentsForTargetPlayer,
                                                                                                shipComponent,
                                                                                                targetedShips)

                    if (allConnectedComponentsAreDestroyed) {
                        shipStatusDao.update(shipComponent.shipId, DESTROYED)
                    }

                    val allTargetedShipsAreDestroyed = targetedShips.flatMap {
                        componentDao.findByPlayerShipId(it.shipId)
                    }.all { it.isDestroyed }

                    if (allTargetedShipsAreDestroyed) {
                        targetingDao.update(targeting.playerId, SEEK)
                    }
                }

                turnDao.save(currentPlayerEntity.id,
                             targetPlayerEntity.id,
                             targetCoordinate.id,
                             shipComponent != null,
                             gameTurn)
            }

        } else gameDao.conclude(currentPlayerEntity.gameId)
    }

    /**
     * Checks if targeted connecting ship components are destroyed.
     *
     * @param componentsForTargetPlayer contains the components to check.
     * @param componentEntity is the component that will not be checked (is not updated yet).
     * @param targetedShipEntities is used to filter the list to the ones currently targeted.
     *
     * @return true if every ship is destroyed, otherwise false.
     */
    private fun allConnectedComponentsAreDestroyed(componentsForTargetPlayer: List<ComponentEntity>,
                                                   componentEntity: ComponentEntity,
                                                   targetedShipEntities: List<TargetedShipEntity>): Boolean {
        return componentsForTargetPlayer
                .filterNot { it.id == componentEntity.id }
                .filter {
                    it.shipId in targetedShipEntities.map { targetedShip ->
                        targetedShip.shipId
                    }
                }.all { it.isDestroyed }
    }

    private fun getShipComponentsForPlayer(playerId: Int): List<ComponentEntity> {
        return shipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            componentDao.findByPlayerShipId(ship.id)
        }
    }

    private fun getPlayersInGame(gameId: Int): Pair<PlayerEntity, PlayerEntity> {
        return playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }
    }
}
