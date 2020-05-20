package no.esa.battleship.repository.player

import no.esa.battleship.service.domain.Player

interface IPlayerDao {

    fun save(gameId: Int): Int
    fun findPlayersInGame(gameId: Int): List<Player>
    fun find(playerId: Int): Player
}
