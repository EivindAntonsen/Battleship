package no.esa.battleship.repository.entity

data class TurnEntity(val id: Int,
                      val gameId: Int,
                      val gameTurn: Int,
                      val playerId: Int,
                      val targetPlayerId: Int,
                      val coordinateEntity: CoordinateEntity,
                      val isHit: Boolean)
