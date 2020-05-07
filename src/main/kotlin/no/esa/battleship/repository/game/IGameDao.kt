package no.esa.battleship.repository.game

import no.esa.battleship.game.Game

interface IGameDao {
    fun find(gameId: Int): Game?
    fun save(): Int
    fun delete(gameId: Int): Int
}
