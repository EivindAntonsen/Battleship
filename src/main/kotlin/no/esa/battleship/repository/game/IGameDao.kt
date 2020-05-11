package no.esa.battleship.repository.game

import no.esa.battleship.service.domain.Game
import java.time.LocalDateTime

interface IGameDao {
    fun save(datetime: LocalDateTime): Int
    fun get(gameId: Int): Game
    fun isGameConcluded(gameId: Int): Boolean
    fun update(game: Game): Int
}
