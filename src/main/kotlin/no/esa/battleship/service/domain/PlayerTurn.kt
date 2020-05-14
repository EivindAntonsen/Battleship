package no.esa.battleship.service.domain

data class PlayerTurn(val id: Int,
                      val gameTurn: Int,
                      val playerId: Int,
                      val coordinate: Coordinate,
                      val isHit: Boolean)
