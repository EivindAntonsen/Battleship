package no.esa.battleship.service.domain

data class PerformanceAnalysis(val player: Player,
                               val shotsFired: Int,
                               val hits: Int,
                               val misses: Int,
                               val hitrate: Double)
