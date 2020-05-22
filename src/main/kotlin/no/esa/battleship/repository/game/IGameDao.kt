package no.esa.battleship.repository.game

import no.esa.battleship.service.domain.Game
import java.time.LocalDateTime
import java.util.*

interface IGameDao {
    fun save(datetime: LocalDateTime): Int
    fun get(gameId: Int): Game
    fun isGameConcluded(gameId: Int): Boolean
    fun conclude(gameId: Int): Int
    fun findGamesInSeries(gameSeriesId: UUID): List<Game>
}
