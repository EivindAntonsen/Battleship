package no.esa.battleship.service.domain

import no.esa.battleship.repository.entity.PlayerEntity

data class PerformanceAnalysis(val playerEntity: PlayerEntity,
                               val shotsFired: Int,
                               val hits: Int,
                               val misses: Int,
                               val hitRate: Double)
