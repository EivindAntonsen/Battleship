package no.esa.battleship.repository.playerboard

import no.esa.battleship.service.domain.PlayerBoard

interface IPlayerBoardDao {
    fun getBoard(playerId: Int): PlayerBoard
}
