package no.esa.battleship.service.game

import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.exceptions.InvalidPerformanceException
import no.esa.battleship.repository.component.IComponentDao
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.entity.ResultEntity
import no.esa.battleship.repository.game.IGameDao
import no.esa.battleship.repository.player.IPlayerDao
import no.esa.battleship.repository.playerstrategy.IPlayerStrategyDao
import no.esa.battleship.repository.result.IResultDao
import no.esa.battleship.repository.shipstatus.IShipStatusDao
import no.esa.battleship.repository.turn.ITurnDao
import no.esa.battleship.service.domain.GameReport
import no.esa.battleship.service.domain.PerformanceAnalysis
import no.esa.battleship.service.domain.PlayerInfo
import org.springframework.stereotype.Service

@Service
class GameService(private val turnDao: ITurnDao,
                  private val gameDao: IGameDao,
                  private val resultDao: IResultDao,
                  private val playerDao: IPlayerDao,
                  private val playerStrategyDao: IPlayerStrategyDao,
                  private val shipStatusDao: IShipStatusDao,
                  private val componentDao: IComponentDao) : IGameService {

    override fun concludeGame(gameId: Int): ResultEntity {
        val game = gameDao.get(gameId)
        gameDao.conclude(gameId)

        val winningPlayer = determineWinningPlayer(game)

        return resultDao.save(gameId, winningPlayer?.id)
    }

    /**
     * Calculates the next game turn based on previous ones.
     *
     * If the latest game turn has two entries (player 1 and player 2),
     * it means both players played their turns for that match,
     * and the next turn will be it + 1.
     */
    override fun getNextGameTurn(gameId: Int): Int {
        val previousTurns = turnDao.getPreviousTurnsByGameId(gameId)
        val latestGameTurn = previousTurns.map {
            it.gameTurn
        }.max()

        return if (latestGameTurn != null) {
            val latestGameTurnIsAlreadyFinished = previousTurns.filter {
                it.gameTurn == latestGameTurn
            }.size == 2

            if (latestGameTurnIsAlreadyFinished) {
                latestGameTurn + 1
            } else latestGameTurn
        } else 1
    }

    override fun getGameReport(game: GameEntity): GameReport {
        val (player1, player2) = getPlayersInGame(game.id)
        val winningPlayer = determineWinningPlayer(game)
        val result = resultDao.save(game.id, winningPlayer?.id)
        val playerInfoList = listOf(getPlayerInfo(player1),
                                    getPlayerInfo(player2))

        return GameReport(game, playerInfoList, result)
    }

    override fun determineWinningPlayer(game: GameEntity): PlayerEntity? {
        val remainingPlayers = componentDao.findRemainingPlayersByGameId(game.id)

        return when {
            remainingPlayers.isEmpty() -> null
            remainingPlayers.size > 1 -> null
            else -> remainingPlayers.first()
        }
    }

    override fun getPlayerInfo(player: PlayerEntity): PlayerInfo {
        val performanceAnalysis = getPerformanceAnalysis(player)
        val strategy = playerStrategyDao.get(player.id)

        return PlayerInfo(player, strategy, performanceAnalysis)
    }

    override fun getPerformanceAnalysis(player: PlayerEntity): PerformanceAnalysis {
        val previousTurns = turnDao.getPreviousTurnsByPlayerId(player.id)
        val hitCount = previousTurns.filter { it.isHit }.count()
        val missCount = previousTurns.filterNot { it.isHit }.count()
        val totalCount = hitCount + missCount

        if (totalCount == 0) throw InvalidPerformanceException(player.id)

        val hitRate = hitCount.toDouble() / totalCount.toDouble()

        return PerformanceAnalysis(player, totalCount, hitCount, missCount, hitRate)
    }

    override fun findRemainingPlayers(gameId: Int): List<PlayerEntity> {
        return componentDao.findRemainingPlayersByGameId(gameId)
    }

    override fun getPlayersInGame(gameId: Int): Pair<PlayerEntity, PlayerEntity> {
        return playerDao.getPlayersInGame(gameId).run {
            first() to last()
        }
    }

    override fun playerHasRemainingShips(playerId: Int): Boolean {
        return shipStatusDao.getAll(playerId).any { (_, status) ->
            status == ShipStatus.INTACT
        }
    }
}
