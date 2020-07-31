package no.esa.battleship.service.domain

import no.esa.battleship.repository.entity.CoordinateEntity

data class TurnRequest(val gameId: Int,
                       val playerId: Int,
                       val targetPlayerId: Int,
                       val coordinateEntity: CoordinateEntity)
