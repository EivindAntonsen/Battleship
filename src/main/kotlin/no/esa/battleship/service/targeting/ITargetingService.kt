package no.esa.battleship.service.targeting

import no.esa.battleship.service.domain.Coordinate

interface ITargetingService {
    fun getTargetCoordinate(playerId: Int, targetPlayerId: Int): Coordinate
}
