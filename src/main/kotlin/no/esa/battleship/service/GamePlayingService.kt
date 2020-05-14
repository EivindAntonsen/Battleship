package no.esa.battleship.service

import no.esa.battleship.enums.Strategy
import no.esa.battleship.repository.boardcoordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerturn.IPlayerTurnDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.service.domain.ShipComponent
import org.springframework.stereotype.Service


/**
 * This service handles the actual playing of a game.
 */
@Service
class GamePlayingService(private val coordinateDao: ICoordinateDao,
                         private val playerDao: IPlayerDao,
                         private val gameDao: IGameDao,
                         private val playerShipDao: IPlayerShipDao,
                         private val playerShipComponentDao: IPlayerShipComponentDao,
                         private val playerStrategyDao: IPlayerStrategyDao,
                         private val playerTurnDao: IPlayerTurnDao) {

    fun playGame(gameId: Int) {
        val (player1, player2) = playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }

        val player1Strategy = playerStrategyDao.find(player1.id)
        val player2Strategy = playerStrategyDao.find(player2.id)

        do {
            executeGameTurn(player1, player1Strategy, player2)
        } while (!gameDao.isGameConcluded(gameId))
    }

    fun executeGameTurn(currentPlayer: Player,
                        currentStrategy: Strategy,
                        targetPlayer: Player) {
        val coordinates = coordinateDao.findAll()

        val unavailableCoordinates = playerTurnDao.getPreviousTurnsForPlayer(currentPlayer.id).map {
            it.coordinate.id
        }
        val availableCoordinates = coordinates.filter { it.id !in unavailableCoordinates }

        val randomCoordinate = availableCoordinates.random()

        //playerTurnDao.save(currentPlayer.id, randomCoordinate.id)
    }

    private fun getShipsForPlayer(playerId: Int): Map<Ship, List<ShipComponent>> {
        return playerShipDao.findAllShipsForPlayer(playerId).map { ship ->
            ship to playerShipComponentDao.findAllComponents(ship.id)
        }.toMap()
    }
}
