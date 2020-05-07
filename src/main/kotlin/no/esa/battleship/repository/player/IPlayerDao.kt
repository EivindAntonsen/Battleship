package no.esa.battleship.repository.player

import no.esa.battleship.game.Player
import java.util.*

interface IPlayerDao {

    fun save(gameId: Int): Int
    fun findPlayersInGame(gameId: Int): List<Player>
}
