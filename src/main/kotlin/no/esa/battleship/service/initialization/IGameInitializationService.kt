package no.esa.battleship.service.initialization

import no.esa.battleship.repository.entity.GameEntity
import java.util.*

interface IGameInitializationService {
    fun initializeNewGame(onlyAI: Boolean): GameEntity
}
