package no.esa.battleship.service.initialization

import no.esa.battleship.service.domain.Game

interface IGameInitializationService {
    fun initializeNewGame(): Game
}
