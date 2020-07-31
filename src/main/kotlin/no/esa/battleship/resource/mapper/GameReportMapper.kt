package no.esa.battleship.resource.mapper

import battleship.model.Game
import battleship.model.GameReportDTO
import battleship.model.PerformanceAnalysis
import battleship.model.Player
import no.esa.battleship.enums.Strategy
import no.esa.battleship.service.domain.GameReport
import java.time.OffsetDateTime
import java.time.ZoneId

object GameReportMapper {

    fun toDTO(gameReport: GameReport): GameReportDTO {
        val game = getGame(gameReport)
        val result = battleship.model.Result(game, gameReport.resultEntity.winningPlayerId)
        val playerPerformance = getPlayerPerformance(gameReport)

        return GameReportDTO(result, playerPerformance)
    }

    private fun getPlayerPerformance(gameReport: GameReport): List<PerformanceAnalysis> {
        return gameReport.playerInfos.map { playerInfo ->
            with(playerInfo.performanceAnalysis) {
                PerformanceAnalysis(playerInfo.playerEntity.id,
                                    shotsFired,
                                    hits,
                                    misses,
                                    hitRate)
            }
        }
    }

    private fun getGame(gameReport: GameReport): Game {
        val zoneId = ZoneId.of("Europe/Paris")
        val offset = zoneId.rules.getOffset(gameReport.gameEntity.dateTime)
        val offsetDateTime = OffsetDateTime.of(gameReport.gameEntity.dateTime, offset)

        return Game(gameReport.gameEntity.id,
                    offsetDateTime,
                    getPlayers(gameReport))
    }

    private fun getPlayers(gameReport: GameReport): List<Player> {
        return gameReport.playerInfos.map { playerInfo ->
            Player(playerInfo.playerEntity.id,
                   when (playerInfo.strategy) {
                       Strategy.RANDOMIZER -> Player.Strategy.RANDOMIZER
                       Strategy.DEFAULT -> Player.Strategy.DEFAULT
                       Strategy.HUMAN -> Player.Strategy.HUMAN
                   })
        }
    }
}
