package no.esa.battleship.repository.player

import no.esa.battleship.repository.entity.PlayerEntity

interface IPlayerDao {

    fun save(gameId: Int): Int
    fun findPlayersInGame(gameId: Int): List<PlayerEntity>
    fun find(playerId: Int): PlayerEntity
}
