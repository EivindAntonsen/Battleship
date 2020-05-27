package no.esa.battleship.service.initialization

import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.GameInitializationException.TooManyPlayers
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.targeting.ITargetingDao
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
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
                                private val targetingModeDao: ITargetingDao,
                                private val playerStrategyDao: IPlayerStrategyDao)
    : IGameInitializationService {

    override fun initializeNewGame(gameSeriesId: UUID?): GameEntity {
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

    private fun newGame(gameSeriesId: UUID? = null): GameEntity {
        val currentTime = LocalDateTime.now()
        val id = gameDao.save(currentTime)

        return GameEntity(id, currentTime, gameSeriesId, false)
    }

    private fun newPlayer(gameEntity: GameEntity): PlayerEntity {
        val currentPlayers = playerDao.findPlayersInGame(gameEntity.id)

        return if (currentPlayers.size in 0..1) {
            val playerId = playerDao.save(gameEntity.id)
            val strategy = Strategy.random()

            logger.info("Selected strategy $strategy for player $playerId.")
            playerStrategyDao.save(playerId, strategy)

            PlayerEntity(playerId, gameEntity.id)
        } else throw TooManyPlayers(gameEntity.id)
    }
}
