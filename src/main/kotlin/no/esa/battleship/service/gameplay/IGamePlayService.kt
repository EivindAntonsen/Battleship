package no.esa.battleship.service.gameplay

import no.esa.battleship.service.domain.GameReport
import no.esa.battleship.repository.entity.PlayerEntity

interface IGamePlayService {
    fun playGame(gameId: Int): GameReport
    fun findRemainingPlayers(gameId: Int): List<PlayerEntity>
}
