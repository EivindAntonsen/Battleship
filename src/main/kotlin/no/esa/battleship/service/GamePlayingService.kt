package no.esa.battleship.service

import no.esa.battleship.repository.boardcoordinate.ICoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
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
                         private val playerStrategyDao: IPlayerStrategyDao) {

    fun playGame(gameId: Int) {
        val (player1, player2) = playerDao.findPlayersInGame(gameId).run {
            first() to last()
        }

        val player1Strategy = playerStrategyDao.find(player1.id)
        val player1Ships = getShipsForPlayer(player1.id)

        val player2Strategy = playerStrategyDao.find(player2.id)
        val player2Ships = getShipsForPlayer(player2.id)

        do {

        } while (!gameDao.isGameConcluded(gameId))
    }

    private fun getShipsForPlayer(playerId: Int): Map<Ship, List<ShipComponent>> {
        return playerShipDao.findAllShipsForPlayer(playerId).map { ship ->
            ship to playerShipComponentDao.findAllComponents(ship.id)
        }.toMap()
    }
}
