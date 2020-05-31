package no.esa.battleship.service.domain

import no.esa.battleship.enums.TargetingMode

data class Targeting(val id: Int,
                     val playerId: Int,
                     val targetPlayerId: Int,
                     val targetingMode: TargetingMode,
                     val targetedShipIds: List<Int>)
