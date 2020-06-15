package no.esa.battleship.repository.game

import no.esa.battleship.repository.entity.GameEntity
import java.time.LocalDateTime
import java.util.*

interface IGameDao {
    fun save(datetime: LocalDateTime): Int
    fun get(gameId: Int): GameEntity
    fun isGameConcluded(gameId: Int): Boolean
    fun conclude(gameId: Int): Int
    fun getGamesInSeries(gameSeriesId: UUID): List<GameEntity>
}
