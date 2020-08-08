package no.esa.battleship.service.gameplay

import battleship.model.TurnRequestDTO
import no.esa.battleship.enums.ShipStatus.DESTROYED
import no.esa.battleship.enums.ShipType
import no.esa.battleship.enums.TargetingMode.DESTROY
import no.esa.battleship.enums.TargetingMode.SEEK
import no.esa.battleship.exceptions.IllegalTurnException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.entity.*
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.ship.IShipDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.*
import no.esa.battleship.service.game.IGameService
import no.esa.battleship.service.targeting.ITargetingService
import no.esa.battleship.utils.executeAndMeasureTimeMillis
import org.slf4j.Logger
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val coordinateDao: ICoordinateDao,
                      private val gameDao: IGameDao,
                      private val shipDao: IShipDao,
                      private val componentDao: IComponentDao,
                      private val shipStatusDao: IShipStatusDao,
                      private val turnDao: ITurnDao,
                      private val targetingService: ITargetingService,
                      private val gameService: IGameService,
                      private val logger: Logger) : IGamePlayService {

    override fun playAiGame(gameId: Int): GameReport {
        val game = gameDao.get(gameId)
        val (player1, player2) = gameService.getPlayersInGame(gameId)

        var gameTurnId = 0

        targetingService.saveInitialTargeting(player1.id, player2.id, gameTurnId)
        targetingService.saveInitialTargeting(player2.id, player1.id, gameTurnId)

        val (_, runtime) = executeAndMeasureTimeMillis {
            do {
                gameTurnId++

                executeGameTurn(player1, player2, gameId, gameTurnId)
            } while (!gameDao.isGameConcluded(gameId))
        }

        logger.info("Concluded game in ${runtime}ms.")

        return gameService.getGameReport(game)
    }

    private fun executeGameTurn(player1: PlayerEntity,
                                player2: PlayerEntity,
                                gameId: Int,
                                gameTurnId: Int) {
        executeAiPlayerTurn(player1, gameId, gameTurnId)
        executeAiPlayerTurn(player2, gameId, gameTurnId)
    }

    private fun findStruckComponent(shipWithComponents: ShipWithComponents, coordinateEntity: CoordinateEntity): ComponentEntity {
        return shipWithComponents.components.first { componentEntity ->
            componentEntity.coordinateEntity == coordinateEntity
        }
    }

    /**
     * This sets the targeting mode to Destroy if it isn't already.
     *
     * This ensures the next strike after a successful hit uses a
     * different targeting algorithm than when it is scoring available
     * coordinates based on ship configurations.
     */
    private fun setTargetingModeToDestroy(targeting: TargetingEntity) {
        if (targeting.targetingMode != DESTROY) {
            logger.info("\t\t\tplayer ${targeting.playerId} - Setting targeting mode to DESTROY.")

            targetingService.updateTargetingMode(targeting.playerId, DESTROY)
        }
    }

    private fun addShipToTargetedShips(targeting: TargetingEntity,
                                       shipId: Int,
                                       targetedShipIds: List<Int>) {
        if (shipId !in targetedShipIds) {
            logger.info("\t\t\tplayer ${targeting.playerId} - Adding ship to targeted ships.")

            targetingService.updateTargetingWithNewShipId(targeting.id, shipId)
        }
    }

    private fun removeShipFromTargetedShipsIfItIsDestroyed(targetingEntity: TargetingEntity,
                                                           shipWithComponents: ShipWithComponents,
                                                           struckCoordinateEntity: CoordinateEntity) {

        val struckShipHasNoIntactComponentsLeft = shipWithComponents.components
                .filter { it.coordinateEntity != struckCoordinateEntity }
                .all { it.isDestroyed }

        if (struckShipHasNoIntactComponentsLeft) {
            logger.info("\t\t\tplayer ${targetingEntity.playerId} - Updating ship status to DESTROYED.")
            shipStatusDao.update(shipWithComponents.ship.id, DESTROYED)

            logger.info("\t\t\tplayer ${targetingEntity.playerId} - Removing ship from targeted ships.")
            targetingService.removeShipIdFromTargeting(targetingEntity.id, shipWithComponents.ship.id)
        }
    }

    private fun setTargetingModeToSeekIfNoTargetedShipRemains(targetingEntity: TargetingEntity) {
        val updatedTargeting = targetingService.getTargeting(targetingEntity.playerId)
        val updatedTargetedShips = targetingService.findTargetedShips(updatedTargeting.id)

        if (updatedTargetedShips.isEmpty()) {
            logger.info("\t\t\tplayer ${targetingEntity.playerId} - Setting targeting mode back to SEEK")
            targetingService.updateTargetingMode(targetingEntity.playerId, SEEK)
        }
    }

    private fun getAllEnemyShips(targetPlayerId: Int): List<ShipEntity> {
        return shipDao.getAllShipsForPlayer(targetPlayerId)
    }

    private fun getTargetedShipIds(targetingEntityId: Int): List<Int> {
        return targetingService.findTargetedShips(targetingEntityId).map { it.id }
    }

    override fun executeHumanPlayerTurn(turnRequest: TurnRequest): TurnResult {
        val previousTurns = turnDao.getPreviousTurnsByGameId(turnRequest.gameId)
        val previousCoordinates = previousTurns.map { it.coordinateEntity }

        if (turnRequest.coordinateEntity in previousCoordinates) throw IllegalTurnException(turnRequest.coordinateEntity.id)

        val targetingEntity = targetingService.getTargeting(turnRequest.playerId)
        val targetedShipIds = targetingService.findTargetedShips(targetingEntity.id).map { it.id }
        val allShipsForTargetPlayer = shipDao.getAllShipsForPlayer(turnRequest.targetPlayerId)
        val struckShip = getStruckShipWithComponents(allShipsForTargetPlayer, turnRequest.coordinateEntity)

        val isHit = if (struckShip != null) {

            setTargetingModeToDestroy(targetingEntity)

            addShipToTargetedShips(targetingEntity, struckShip.ship.id, targetedShipIds)

            updateStruckComponentToDestroyedStatus(struckShip, turnRequest.coordinateEntity)

            removeShipFromTargetedShipsIfItIsDestroyed(targetingEntity, struckShip, turnRequest.coordinateEntity)

            setTargetingModeToSeekIfNoTargetedShipRemains(targetingEntity)

            true
        } else false

        turnDao.save(turnRequest.gameId,
                     turnRequest.playerId,
                     turnRequest.targetPlayerId,
                     turnRequest.coordinateEntity.id,
                     isHit,
                     gameService.getNextGameTurn(turnRequest.gameId))

        val struckShipIsDestroyed = struckShip?.let {
            isStruckShipDestroyed(it, turnRequest.coordinateEntity)
        } ?: false

        return TurnResult(turnRequest.coordinateEntity, isHit, struckShipIsDestroyed)
    }

    private fun executeAiPlayerTurn(currentPlayer: PlayerEntity, gameId: Int, gameTurn: Int) {
        val currentPlayerIsOutOfShips = !gameService.playerHasRemainingShips(currentPlayer.id)

        if (currentPlayerIsOutOfShips) {
            gameDao.conclude(gameId)
            return
        }

        val targetingEntity = targetingService.getTargeting(currentPlayer.id)
        val targetCoordinate = targetingService.getTargetCoordinate(targetingEntity)
        val targetedShipIds = getTargetedShipIds(targetingEntity.id)
        val allEnemyShips = getAllEnemyShips(targetingEntity.targetPlayerId)
        val struckShipWithComponents = getStruckShipWithComponents(allEnemyShips, targetCoordinate)
        val isHit = struckShipWithComponents != null

        if (isHit) {
            logger.info("Turn $gameTurn,\tplayer ${targetingEntity.playerId} - shot was a HIT.")

            val struckComponent = findStruckComponent(struckShipWithComponents!!, targetCoordinate)

            setTargetingModeToDestroy(targetingEntity)
            addShipToTargetedShips(targetingEntity, struckComponent.shipId, targetedShipIds)
            componentDao.update(struckComponent.id, true)
            removeShipFromTargetedShipsIfItIsDestroyed(targetingEntity, struckShipWithComponents, targetCoordinate)
            setTargetingModeToSeekIfNoTargetedShipRemains(targetingEntity)
        }

        turnDao.save(gameId,
                     targetingEntity.playerId,
                     targetingEntity.targetPlayerId,
                     targetCoordinate.id,
                     isHit,
                     gameTurn)
    }

    override fun continueGame(turnRequest: TurnRequestDTO): TurnResult {
        val coordinateEntity = coordinateDao.getAll().first { it.id == turnRequest.coordinateId }
        val request = TurnRequest(turnRequest.gameId,
                                  turnRequest.playerId,
                                  turnRequest.targetPlayerId,
                                  coordinateEntity)

        return executeHumanPlayerTurn(request)
    }

    private fun updateStruckComponentToDestroyedStatus(struckShip: ShipWithComponents, coordinateEntity: CoordinateEntity) {
        val struckComponentId = struckShip.components.first { component ->
            component.coordinateEntity == coordinateEntity
        }.id

        componentDao.update(struckComponentId, true)
    }

    private fun isStruckShipDestroyed(struckShip: ShipWithComponents, coordinateEntity: CoordinateEntity): Boolean {
        return componentDao.getByShipId(struckShip.ship.id).filterNot { componentEntity ->
            componentEntity.coordinateEntity == coordinateEntity
        }.all { it.isDestroyed }
    }

    /**
     * Returns a ship with its components if a component coordinate matches the target coordinate.
     */
    private fun getStruckShipWithComponents(enemyShips: List<ShipEntity>,
                                            targetCoordinate: CoordinateEntity): ShipWithComponents? {
        return enemyShips.firstOrNull { ship ->
            componentDao.getByShipId(ship.id).any { componentEntity ->
                componentEntity.coordinateEntity == targetCoordinate
            }
        }?.let { ship ->
            val componentEntities = componentDao.getByShipId(ship.id)
            val shipType = ShipType.fromInt(ship.shipTypeId)
            val components = Components(shipType, componentEntities)

            ShipWithComponents(ship, components)
        }
    }
}
