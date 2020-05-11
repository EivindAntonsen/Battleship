package no.esa.battleship.service.domain

import no.esa.battleship.enums.ShotDirection

data class PlayerTurn(val id: Int,
                      val gameTurn: Int,
                      val playerBoardId: Int,
                      val coordinate: Coordinate,
                      val shotDirection: ShotDirection,
                      val isHit: Boolean)
