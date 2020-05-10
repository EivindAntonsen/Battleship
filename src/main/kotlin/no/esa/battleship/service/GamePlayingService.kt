package no.esa.battleship.service

import no.esa.battleship.repository.boardcoordinate.BoardCoordinateDao
import no.esa.battleship.repository.boardcoordinate.IBoardCoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerboard.IPlayerBoardDao
import no.esa.battleship.repository.playerboardhistory.IPlayerBoardHistoryDao
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
class GamePlayingService(private val gameDao: IGameDao,
                         private val boardCoordinateDao: IBoardCoordinateDao,
                         private val playerDao: IPlayerDao,
                         private val playerShipDao: IPlayerShipDao,
                         private val playerShipComponentDao: IPlayerShipComponentDao,
                         private val playerBoardDao: IPlayerBoardDao,
                         private val playerBoardHistoryDao: IPlayerBoardHistoryDao,
                         private val playerStrategyDao: IPlayerStrategyDao) {

    fun playGame(gameId: Int) {
        val boardCoordinates = boardCoordinateDao.findAll()

        val playersAndStrategies = playerDao.findPlayersInGame(gameId).map { player ->
            player to playerStrategyDao.find(player.id)
        }.toMap()

        val playersAndShipsWithComponents: List<Map<Ship, List<ShipComponent>>> = playersAndStrategies.keys.map { player ->
            playerShipDao.findAllShipsForPlayer(player.id).map { ship ->
                ship to playerShipComponentDao.findAllComponents(ship.id)
            }.toMap()
        }
    }
}
