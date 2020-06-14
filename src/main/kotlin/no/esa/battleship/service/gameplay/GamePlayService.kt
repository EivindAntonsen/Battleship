package no.esa.battleship.service.gameplay

import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipStatus.INTACT
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.InvalidPerformanceException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.entity.ShipEntity
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.*
import no.esa.battleship.service.targeting.ITargetingService
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val playerDao: IPlayerDao,
                      private val gameDao: IGameDao,
                      private val shipDao: IShipDao,
                      private val componentDao: IComponentDao,
                      private val shipStatusDao: IShipStatusDao,
                      private val turnDao: ITurnDao,
                      private val playerStrategyDao: IPlayerStrategyDao,
                      private val resultDao: IResultDao,
                      private val targetingService: ITargetingService,
                      private val logger: Logger) : IGamePlayService {

    override fun playGame(gameId: Int): GameReport {
        val game = gameDao.get(gameId)
        val (player1, player2) = getPlayersInGame(gameId)

        var gameTurnId = 0

        targetingService.saveInitialTargeting(player1.id, player2.id, gameTurnId)
        targetingService.saveInitialTargeting(player2.id, player1.id, gameTurnId)

        do {
            gameTurnId++

            executeGameTurn(player1, player2, gameTurnId)
        } while (!gameDao.isGameConcluded(gameId))

        return getGameReport(game)
    }

    private fun executeGameTurn(player1: PlayerEntity,
                                player2: PlayerEntity,
                                gameTurnId: Int) {

        executePlayerTurn(player1, gameTurnId)
        executePlayerTurn(player2, gameTurnId)
    }

    private fun getGameReport(game: GameEntity): GameReport {
        val (player1, player2) = getPlayersInGame(game.id)
        val playerInfoList = listOf(getPlayerInfo(player1), getPlayerInfo(player2))
        val winningPlayer = determineWinningPlayer(game)
        val resultEntity = resultDao.save(game.id, winningPlayer?.id)

        return GameReport(game, playerInfoList, resultEntity)
    }

    private fun determineWinningPlayer(game: GameEntity): PlayerEntity? {
        val remainingPlayers = findRemainingPlayers(game.id)

        return when {
            remainingPlayers.size > 1 -> null
            remainingPlayers.isEmpty() -> null
            else -> remainingPlayers.first()
        }
    }

    private fun getPlayerInfo(player: PlayerEntity): PlayerInfo {
        return PlayerInfo(player,
                          playerStrategyDao.find(player.id),
                          getPerformanceAnalysis(player))
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

    private fun executePlayerTurn(currentPlayer: PlayerEntity, gameTurn: Int) {
        if (playerHasRemainingShips(currentPlayer.id)) {
            val targeting = targetingService.getTargeting(currentPlayer.id)
            val targetCoordinate = targetingService.getTargetCoordinate(targeting)
            val targetedShips = targetingService.findTargetedShips(targeting.id)
            val targetedShipIds = targetedShips.map { it.shipId }
            val allEnemyShips = shipDao.findAllShipsForPlayer(targeting.targetPlayerId)
            val struckShipWithComponents = getStruckShipWithComponents(allEnemyShips, targetCoordinate)

            if (struckShipWithComponents != null) {
                logger.info("Turn $gameTurn,\tplayer ${targeting.playerId} - shot was a HIT.")
                val struckComponent = struckShipWithComponents.components.first { componentEntity ->
                    componentEntity.coordinateEntity == targetCoordinate
                }

                if (targeting.targetingMode != DESTROY) {
                    logger.info("\t\t\tplayer ${targeting.playerId} - Setting targeting mode to DESTROY.")
                    targetingService.updateTargetingMode(targeting.playerId, DESTROY)
                }

                if (struckComponent.shipId !in targetedShipIds) {
                    logger.info("\t\t\tplayer ${targeting.playerId} - Adding ship to targeted ships.")
                    targetingService.updateTargetingWithNewShipId(targeting.id, struckShipWithComponents.ship.id)
                }

                componentDao.update(struckComponent.id, true)

                val updatedShipWithComponents = getShipWithComponents(struckShipWithComponents.ship.id)
                val struckShipHasNoIntactComponentsLeft = updatedShipWithComponents.components.all { it.isDestroyed }

                if (struckShipHasNoIntactComponentsLeft) {
                    logger.info("\t\t\tplayer ${targeting.playerId} - Updating ship status to DESTROYED.")
                    shipStatusDao.update(updatedShipWithComponents.ship.id, DESTROYED)
                    logger.info("\t\t\tplayer ${targeting.playerId} - Removing ship from targeted ships.")
                    targetingService.removeShipIdFromTargeting(targeting.id, struckComponent.shipId)
                }

                val updatedTargeting = targetingService.getTargeting(targeting.playerId)
                val updatedTargetedShips = targetingService.findTargetedShips(updatedTargeting.id)

                if (updatedTargetedShips.isEmpty()) {
                    logger.info("\t\t\tplayer ${targeting.playerId} - Setting targeting mode back to SEEK")
                    targetingService.updateTargetingMode(targeting.playerId, SEEK)
                }

                turnDao.save(targeting.playerId,
                             targeting.targetPlayerId,
                             targetCoordinate.id,
                             true,
                             gameTurn)
            } else turnDao.save(targeting.playerId,
                                targeting.targetPlayerId,
                                targetCoordinate.id,
                                false,
                                gameTurn)
        } else {
            logger.info("\t\t\tplayer ${currentPlayer.id} wins!")
            gameDao.conclude(currentPlayer.gameId)
        }
    }

    private fun getShipWithComponents(id: Int): ShipWithComponents {
        val shipEntity = shipDao.find(id)
        val componentEntities = componentDao.findByPlayerShipId(shipEntity.id)
        val components = Components(ShipType.fromInt(shipEntity.shipTypeId),
                                    componentEntities)

        return ShipWithComponents(shipEntity, components)
    }

    private fun playerHasRemainingShips(playerId: Int): Boolean {
        return shipStatusDao.findAll(playerId).any { (_, status) ->
            status == INTACT
        }
    }

    private fun getPlayersInGame(gameId: Int): Pair<PlayerEntity, PlayerEntity> {
        return playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }
    }

    private fun getStruckShipWithComponents(enemyShips: List<ShipEntity>,
                                            targetCoordinate: CoordinateEntity): ShipWithComponents? {
        return enemyShips.firstOrNull { ship ->
            componentDao.findByPlayerShipId(ship.id).any { componentEntity ->
                componentEntity.coordinateEntity == targetCoordinate
            }
        }?.let { ship ->
            val componentEntities = componentDao.findByPlayerShipId(ship.id)
            val shipType = ShipType.fromInt(ship.shipTypeId)
            val components = Components(shipType, componentEntities)

            ShipWithComponents(ship, components)
        }
    }
}
