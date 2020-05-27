package no.esa.battleship.service.initialization

import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.GameInitializationException.TooManyPlayers
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.playertargeting.IPlayerTargetingDao
import no.esa.battleship.service.domain.Game
import no.esa.battleship.service.domain.Player
import no.esa.battleship.service.shipplacement.IShipPlacementService
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class GameInitializationService(private val logger: Logger,
                                private val gameDao: IGameDao,
                                private val playerDao: IPlayerDao,
                                private val shipPlacementService: IShipPlacementService,
                                private val playerTargetingModeDao: IPlayerTargetingDao,
                                private val playerStrategyDao: IPlayerStrategyDao)
    : IGameInitializationService {

    override fun initializeNewGame(gameSeriesId: UUID?): Game {
        return logger.log {
            newGame(gameSeriesId).also { game ->
                repeat(2) {
                    newPlayer(game).also { player ->
                        shipPlacementService.placeShipsForPlayer(player.id)
                    }
                }
            }
        }
    }

    private fun newGame(gameSeriesId: UUID? = null): Game {
        val currentTime = LocalDateTime.now()
        val id = gameDao.save(currentTime)

        return Game(id, currentTime, gameSeriesId, false)
    }

    private fun newPlayer(game: Game): Player {
        val currentPlayers = playerDao.findPlayersInGame(game.id)

        return if (currentPlayers.size in 0..1) {
            val playerId = playerDao.save(game.id)
            val strategy = Strategy.random()

            logger.info("Selected strategy $strategy for player $playerId.")
            playerStrategyDao.save(playerId, strategy)

            Player(playerId, game.id)
        } else throw TooManyPlayers(game.id)
    }
}
