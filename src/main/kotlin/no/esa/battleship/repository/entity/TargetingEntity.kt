package no.esa.battleship.repository.entity

import no.esa.battleship.enums.TargetingMode

data class TargetingEntity(val id: Int,
                           val playerId: Int,
                           val targetPlayerId: Int,
                           val targetingMode: TargetingMode)
