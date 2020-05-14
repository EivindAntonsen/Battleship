package no.esa.battleship.service

import no.esa.battleship.exceptions.GameInitialization.TooManyPlayersException
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.service.domain.Game
import no.esa.battleship.service.domain.Player
import no.esa.battleship.utils.log
import no.esa.battleship.utils.toCamelCase
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.time.LocalDateTime
import java.util.*

@Service
class GameInitializationService(private val logger: Logger,
                                private val gameDao: IGameDao,
                                private val playerDao: IPlayerDao,
                                private val shipPlacementService: ShipPlacementService,
                                @Qualifier("errorMessages") private val resourceBundle: ResourceBundle)
    : IGameInitializationService {

    override fun initializeNewGame(): Game {
        return logger.log {
            val game = newGame()

            val player1 = newPlayer(game)
            shipPlacementService.placeShipsForPlayer(player1.id)
            val player2 = newPlayer(game)
            shipPlacementService.placeShipsForPlayer(player2.id)

            game
        }
    }

    private fun newGame(): Game {
        val currentTime = LocalDateTime.now()
        val id = gameDao.save(currentTime)

        return Game(id, currentTime, false)
    }

    private fun newPlayer(game: Game): Player {
        val currentPlayers = playerDao.findPlayersInGame(game.id)

        return if (currentPlayers.size in 0..1) {
            val playerId = playerDao.save(game.id)

            Player(playerId, game.id)
        } else throw TooManyPlayersException(game.id)
    }
}
