package no.esa.battleship.service.initialization

import no.esa.battleship.service.domain.Game
import java.util.*

interface IGameInitializationService {
    fun initializeNewGame(gameSeriesId: UUID?): Game
}
