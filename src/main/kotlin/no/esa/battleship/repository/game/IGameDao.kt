package no.esa.battleship.repository.game

import no.esa.battleship.game.Game
import java.time.LocalDateTime

interface IGameDao {
    fun save(datetime: LocalDateTime): Int
}
