package no.esa.battleship.repository.playertargetingmode

import no.esa.battleship.enums.TargetingMode

interface IPlayerTargetingModeDao {
    fun find(playerId: Int): TargetingMode
    fun update(playerId: Int, targetingMode: TargetingMode): Int
    fun save(playerId: Int): Int
}
