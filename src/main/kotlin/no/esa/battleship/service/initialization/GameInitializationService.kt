package no.esa.battleship.service.initialization

import no.esa.battleship.enums.PlayerType
import no.esa.battleship.enums.PlayerType.AI
import no.esa.battleship.enums.PlayerType.HUMAN
import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.TooManyPlayersException
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.service.shipplacement.IShipPlacementService
import org.slf4j.Logger
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GameInitializationService(private val logger: Logger,
                                private val gameDao: IGameDao,
                                private val playerDao: IPlayerDao,
                                private val shipPlacementService: IShipPlacementService,
                                private val playerStrategyDao: IPlayerStrategyDao) : IGameInitializationService {

    /**
     * Initializes a new game.
     *
     * @param onlyAI if true it creates a game for two AI players,
     *               and populates the board with their ships.
     *               Otherwise it creates a game for two players,
     *               and populates the board with ships for the AI player.
     */
    override fun initializeNewGame(onlyAI: Boolean): GameEntity {
        val game = newGame()
        val aiPlayer = newPlayer(game, AI)

        shipPlacementService.placeShipsForPlayer(aiPlayer.id)
        if (onlyAI) shipPlacementService.placeShipsForPlayer(newPlayer(game, AI).id) else newPlayer(game, HUMAN)

        return game
    }

    private fun newGame(): GameEntity {
        val currentTime = LocalDateTime.now()
        val id = gameDao.save(currentTime)

        return GameEntity(id, currentTime, false)
    }

    /**
     * Adds a new player to an existing game as well as a strategy.
     *
     * If the player type is AI, the matching
     */
    private fun newPlayer(game: GameEntity, playerType: PlayerType = AI): PlayerEntity {
        val currentPlayers = playerDao.getPlayersInGame(game.id)

        return if (currentPlayers.size in 0..1) {
            val playerId = playerDao.save(game.id, playerType.id)
            val strategy = if (playerType == AI) Strategy.DEFAULT.also {
                logger.info("Selected strategy $it for player $playerId.")
            } else Strategy.HUMAN

            playerStrategyDao.save(playerId, strategy)

            PlayerEntity(playerId, playerType.id, game.id)
        } else throw TooManyPlayersException(game.id)
    }
}
