package no.esa.battleship.service.domain

import no.esa.battleship.enums.Strategy

data class PlayerInfo(val player: Player,
                      val strategy: Strategy,
                      val performanceAnalysis: PerformanceAnalysis)
