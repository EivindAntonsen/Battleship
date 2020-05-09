package no.esa.battleship.service

import no.esa.battleship.service.domain.Game

interface IGameInitializationService {
    fun initializeNewGame(): Game
}
