package no.esa.battleship.service

import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.InvalidGameStateException
import no.esa.battleship.repository.boardcoordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.domain.ShipComponent
import org.slf4j.Logger
import org.springframework.stereotype.Service


/**
 * This service handles the actual playing of a game.
 */
@Service
class GamePlayingService(private val logger: Logger,
                         private val coordinateDao: ICoordinateDao,
                         private val playerDao: IPlayerDao,
                         private val gameDao: IGameDao,
                         private val playerShipDao: IPlayerShipDao,
                         private val playerShipComponentDao: IPlayerShipComponentDao,
                         private val playerStrategyDao: IPlayerStrategyDao,
                         private val playerTurnDao: IPlayerTurnDao) {

    fun playGame(gameId: Int): Player? {
        val (player1, player2) = getPlayersInGame(gameId)
        val player1Strategy = playerStrategyDao.find(player1.id)
        val player2Strategy = playerStrategyDao.find(player2.id)

        var counter = 1

        do {
            executeGameTurn(player1, player1Strategy, player2, counter)
            executeGameTurn(player2, player2Strategy, player1, counter)

            counter++
        } while (!gameDao.isGameConcluded(gameId))

        val remainingPlayers = playerShipComponentDao.findRemainingShipComponents().map {
            val ship = playerShipDao.find(it.playerShipId)

            playerDao.find(ship.playerId)
        }.distinct()

        return when {
            remainingPlayers.size > 1 -> null
            remainingPlayers.isEmpty() -> null
            else -> remainingPlayers.first()
        }
    }

    fun executeGameTurn(currentPlayer: Player,
                        currentStrategy: Strategy,
                        targetPlayer: Player,
                        gameTurn: Int) {

        if (isPlayerFleetAlive(currentPlayer.id)) {
            if (gameTurn > 1200) throw InvalidGameStateException("Game should have been concluded by now!")

            val availableCoordinates = getAvailableCoordinatesForPlayer(currentPlayer.id)
            val targetCoordinate = availableCoordinates.random()
            val targetPlayerShipComponents = getShipComponentsForPlayer(targetPlayer.id)

            val isHit = targetPlayerShipComponents.any { shipComponent ->
                shipComponent.coordinate.id == targetCoordinate.id
            }

            if (isHit) {
                logger.info("Coordinate $targetCoordinate is a hit!")

                val shipComponentToUpdateAsDestroyed = targetPlayerShipComponents.first {
                    it.coordinate.id == targetCoordinate.id
                }

                playerShipComponentDao.update(shipComponentToUpdateAsDestroyed.id, true)
            }

            playerTurnDao.save(currentPlayer.id, targetCoordinate.id, isHit, gameTurn)
        } else gameDao.conclude(currentPlayer.gameId)
    }

    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<Coordinate> {
        val unavailableCoordinateIds = playerTurnDao.getPreviousTurnsForPlayer(playerId).map { turn ->
            turn.coordinate
        }

        val availableCoordinates = coordinateDao.findAll().filter { coordinate ->
            coordinate !in unavailableCoordinateIds
        }

        return if (availableCoordinates.isNotEmpty()) {
            availableCoordinates
        } else throw InvalidGameStateException("No valid coordinates left for player $playerId!")
    }

    private fun isPlayerFleetAlive(playerId: Int): Boolean {
        return playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findAllComponents(ship.id)
        }.any { !it.isDestroyed }
    }

    private fun getShipComponentsForPlayer(playerId: Int): List<ShipComponent> {
        return playerShipDao.findAllShipsForPlayer(playerId).flatMap { ship ->
            playerShipComponentDao.findAllComponents(ship.id)
        }
    }

    private fun getPlayersInGame(gameId: Int): Pair<Player, Player> {
        return playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }
    }
}
