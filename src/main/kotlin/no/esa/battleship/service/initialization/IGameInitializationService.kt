package no.esa.battleship.service.initialization

import no.esa.battleship.repository.entity.GameEntity

interface IGameInitializationService {
    fun initializeNewGame(onlyAI: Boolean): GameEntity
}
