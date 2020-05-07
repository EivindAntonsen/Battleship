package no.esa.battleship.service

import no.esa.battleship.exceptions.TooManyPlayersException
import no.esa.battleship.game.Game
import no.esa.battleship.game.Player
import no.esa.battleship.repository.boardcoordinate.IBoardCoordinateDao
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerboardhistory.IPlayerBoardHistoryDao
import no.esa.battleship.repository.playership.IPlayerShipDao
import no.esa.battleship.repository.playershipcomponent.IPlayerShipComponentDao
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GameService(private val boardCoordinateDao: IBoardCoordinateDao,
                  private val gameDao: IGameDao,
                  private val playerDao: IPlayerDao,
                  private val playerBoardHistoryDao: IPlayerBoardHistoryDao,
                  private val playerShipDao: IPlayerShipDao,
                  private val playerShipComponentDao: IPlayerShipComponentDao) {

    fun newGame(): Game {
        val currentTime = LocalDateTime.now()
        val id = gameDao.save(currentTime)

        return Game(id, currentTime)
    }

    fun newPlayer(game: Game): Player {
        val currentPlayers = playerDao.findPlayersInGame(game.id)

        return if (currentPlayers.size in 0..1) {
            val playerId = playerDao.save(game.id)

            Player(playerId, game.id)
        } else throw TooManyPlayersException(game.id)
    }
}
