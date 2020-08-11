package no.esa.battleship.service.game

import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.entity.ResultEntity
import no.esa.battleship.repository.entity.TurnEntity
import no.esa.battleship.service.domain.GameReport
import no.esa.battleship.service.domain.PerformanceAnalysis
import no.esa.battleship.service.domain.PlayerInfo

interface IGameService {
    fun getNextGameTurn(gameId: Int): Int
    fun getGameReport(game: GameEntity): GameReport
    fun determineWinningPlayer(game: GameEntity): PlayerEntity?
    fun getPlayerInfo(player: PlayerEntity): PlayerInfo
    fun getPerformanceAnalysis(player: PlayerEntity): PerformanceAnalysis
    fun findRemainingPlayers(gameId: Int): List<PlayerEntity>
    fun getPlayersInGame(gameId: Int): Pair<PlayerEntity, PlayerEntity>
    fun playerHasRemainingShips(playerId: Int): Boolean
    fun concludeGame(gameId: Int): ResultEntity
}
