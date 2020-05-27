package no.esa.battleship.service.targeting

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.PlayerTargetedShip
import no.esa.battleship.service.domain.PlayerTargeting

interface ITargetingService {
    fun getTargetCoordinate(targeting: PlayerTargeting,
                            targetedShips: List<PlayerTargetedShip>): Coordinate

    fun getPlayerTargeting(playerId: Int): Pair<PlayerTargeting, List<PlayerTargetedShip>>
}
