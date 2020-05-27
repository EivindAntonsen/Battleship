package no.esa.battleship.repository.result

import no.esa.battleship.repository.entity.ResultEntity

interface IResultDao {
    fun save(gameId: Int, winningPlayerId: Int?): ResultEntity
    fun get(gameId: Int): ResultEntity
    fun getAll()
}
