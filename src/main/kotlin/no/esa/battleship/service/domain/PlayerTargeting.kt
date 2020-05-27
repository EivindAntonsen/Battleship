package no.esa.battleship.service.domain

import no.esa.battleship.enums.TargetingMode

data class PlayerTargeting(val id: Int,
                           val playerId: Int,
                           val targetPlayerId: Int,
                           val targetingMode: TargetingMode)
