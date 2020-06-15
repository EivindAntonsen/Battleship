package no.esa.battleship.repository.player

import no.esa.battleship.repository.entity.PlayerEntity

interface IPlayerDao {

    fun save(gameId: Int): Int
    fun getPlayersInGame(gameId: Int): List<PlayerEntity>
    fun get(playerId: Int): PlayerEntity
}
