package no.esa.battleship.repository.player

import no.esa.battleship.game.Player
import java.util.*

interface IPlayerDao {

    fun save(player: Player): Int
    fun find(uuid: UUID): Player?
    fun findByGameId(gameId: Int): List<Player>
    fun delete(uuid: UUID): Int
    fun deleteByGameId(gameId: Int): Int
}
