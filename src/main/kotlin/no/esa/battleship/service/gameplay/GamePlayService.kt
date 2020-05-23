package no.esa.battleship.service.gameplay

import no.esa.battleship.exceptions.NoValidCoordinatesException
import no.esa.battleship.repository.coordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.domain.Result
import no.esa.battleship.service.domain.ShipComponent
import no.esa.battleship.utils.isAdjacentWith
import org.springframework.stereotype.Service

@Service
class GamePlayService(private val coordinateDao: ICoordinateDao,
                      private val playerDao: IPlayerDao,
                      private val gameDao: IGameDao,
                      private val playerShipDao: IPlayerShipDao,
                      private val playerShipComponentDao: IPlayerShipComponentDao,
                      private val playerTurnDao: IPlayerTurnDao,
                      private val resultDao: IResultDao) : IGamePlayService {

    override fun playGame(gameId: Int): Result {
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

        return resultDao.save(gameId, winningPlayer?.id)
    }

    override fun findRemainingPlayers(gameId: Int): List<Player> {
        return playerShipComponentDao.findByGameId(gameId).map { shipComponent ->
            val ship = playerShipDao.find(shipComponent.playerShipId)

            playerDao.find(ship.playerId)
        }.distinct()
    }


    /**
     * This function calculates the probable minimum distance between
     * shots to account for any destroyed ship. i.e. if the smallest ship
     * has been verified destroyed (2 coordinates long),
     * the new minimum distance between neighbouring coordinates is now 2 coordinates.
     */
    override fun calculateProbableMinimumDistance(playerId: Int): Int {
        val previousHits = playerTurnDao.getPreviousTurnsForPlayer(playerId).filter { it.isHit }

        TODO() // finish this function
    }

    private fun executeGameTurn(currentPlayer: Player,
                                targetPlayer: Player,
                                gameTurn: Int) {

        if (playerFleetIsAlive(currentPlayer.id)) {
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
