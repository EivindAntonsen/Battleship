package no.esa.battleship.repository.player

import no.esa.battleship.repository.model.Player

interface IPlayerDao {

    fun save(gameId: Int): Int
    fun findPlayersInGame(gameId: Int): List<Player>
}
