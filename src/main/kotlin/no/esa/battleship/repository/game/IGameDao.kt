package no.esa.battleship.repository.game

import java.time.LocalDateTime

interface IGameDao {
    fun save(datetime: LocalDateTime): Int
}
