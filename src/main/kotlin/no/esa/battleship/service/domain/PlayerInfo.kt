package no.esa.battleship.service.domain

import no.esa.battleship.enums.Strategy
import no.esa.battleship.repository.entity.PlayerEntity

data class PlayerInfo(val playerEntity: PlayerEntity,
                      val strategy: Strategy,
                      val performanceAnalysis: PerformanceAnalysis)
