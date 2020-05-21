package no.esa.battleship.repository.result

import no.esa.battleship.service.domain.Result

interface IResultDao {
    fun save(gameId: Int, winningPlayerId: Int?): Result
    fun get(gameId: Int): Result
    fun getAll()
}
