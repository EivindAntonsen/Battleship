package no.esa.battleship.resource.mapper

import no.esa.battleship.enums.Strategy
import no.esa.battleship.model.*
import no.esa.battleship.service.domain.GameReport
import java.time.OffsetDateTime
import java.time.ZoneId

object GameReportMapper {

    fun toDTO(gameReport: GameReport): GameReportDTO {
        val game = getGame(gameReport)
        val result = Result(game, gameReport.result.winningPlayerId)
        val playerPerformance = getPlayerPerformance(gameReport)

        return GameReportDTO(result, playerPerformance)
    }

    private fun getPlayerPerformance(gameReport: GameReport): List<PerformanceAnalysis> {
        return gameReport.playerInfos.map { playerInfo ->
            with(playerInfo.performanceAnalysis) {
                PerformanceAnalysis(playerInfo.player.id,
                                    shotsFired,
                                    hits,
                                    misses,
                                    hitrate)
            }
        }
    }

    private fun getGame(gameReport: GameReport): Game {
        val zoneId = ZoneId.of("Europe/Paris")
        val offset = zoneId.rules.getOffset(gameReport.game.dateTime)
        val offsetDateTime = OffsetDateTime.of(gameReport.game.dateTime, offset)

        return Game(gameReport.game.id,
                    offsetDateTime,
                    getPlayers(gameReport))
    }

    private fun getPlayers(gameReport: GameReport): List<Player> {
        return gameReport.playerInfos.map { playerInfo ->
            Player(playerInfo.player.id,
                   when (playerInfo.strategy) {
                       Strategy.RANDOMIZER -> Player.Strategy.RANDOMIZER
                       Strategy.DEFAULT -> Player.Strategy.DEFAULT
                       Strategy.MATHEMATICIAN -> Player.Strategy.MATHEMATICIAN
                   })
        }
    }
}
