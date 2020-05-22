package no.esa.battleship.service.analysis

import java.util.*

interface IAnalysisService {
    fun analyseGames(amount: Int): UUID
}
