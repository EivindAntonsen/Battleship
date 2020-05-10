package no.esa.battleship.exceptions

sealed class GameInitialization(message: String) : RuntimeException(message) {
    class ShipPlacementException(message: String) : GameInitialization(message)
    class TooManyPlayersException(gameId: Int) : GameInitialization("game $gameId already has the max amount of players (2)!")
}
