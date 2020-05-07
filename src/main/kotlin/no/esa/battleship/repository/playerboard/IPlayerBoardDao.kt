package no.esa.battleship.repository.playerboard

import no.esa.battleship.repository.model.PlayerBoard

interface IPlayerBoardDao {
    fun getBoard(playerId: Int): PlayerBoard
}
