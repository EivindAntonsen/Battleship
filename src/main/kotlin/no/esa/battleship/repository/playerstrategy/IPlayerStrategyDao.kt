package no.esa.battleship.repository.playerstrategy

import no.esa.battleship.enums.Strategy

interface IPlayerStrategyDao {
    fun save(playerId: Int, strategy: Strategy): Int
    fun find(playerId: Int): Strategy
}
