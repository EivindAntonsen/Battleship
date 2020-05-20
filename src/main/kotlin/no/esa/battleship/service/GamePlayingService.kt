package no.esa.battleship.service

import no.esa.battleship.exceptions.InvalidGameStateException
import no.esa.battleship.repository.boardcoordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
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
                         private val playerTurnDao: IPlayerTurnDao) {

    fun playGame(gameId: Int): Player? {
        val (player1, player2) = getPlayersInGame(gameId)
        var gameTurnId = 1

        do {
            executeGameTurn(player1, player2, gameTurnId)
            executeGameTurn(player2, player1, gameTurnId)

            gameTurnId++
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

    private fun executeGameTurn(currentPlayer: Player,
                                targetPlayer: Player,
                                gameTurn: Int) {

        if (isPlayerFleetAlive(currentPlayer.id)) {
            if (gameTurn > 100) throw InvalidGameStateException("Game should have been concluded by now!")

            val availableCoordinates = getAvailableCoordinatesForPlayer(currentPlayer.id)
            val targetCoordinateId = availableCoordinates.random()

            getShipComponentsForPlayer(targetPlayer.id).firstOrNull { component ->
                component.coordinate.id == targetCoordinateId
            }.let { shipComponent ->
                if (shipComponent != null) {
                    logger.info("Coordinate $targetCoordinateId is a hit!")

                    playerShipComponentDao.update(shipComponent.id, true)
                }

                playerTurnDao.save(currentPlayer.id,
                                   targetCoordinateId,
                                   shipComponent != null,
                                   gameTurn)
            }

        } else gameDao.conclude(currentPlayer.gameId)
    }

    private fun getAvailableCoordinatesForPlayer(playerId: Int): List<Int> {
        val unavailableCoordinateIds = playerTurnDao.getPreviousTurnsForPlayer(playerId).map { turn ->
            turn.coordinate.id
        }

        return coordinateDao.findAll()
                .map { it.id }
                .filterNot { it in unavailableCoordinateIds }
                .ifEmpty { throw InvalidGameStateException("No valid coordinates left for player $playerId!") }
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
