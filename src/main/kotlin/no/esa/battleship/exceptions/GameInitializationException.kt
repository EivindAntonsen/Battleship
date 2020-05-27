package no.esa.battleship.exceptions

sealed class GameInitializationException(message: String) : RuntimeException(message) {
    class ShipPlacement(message: String) : GameInitializationException(message)
    class TooManyPlayers(gameId: Int) : GameInitializationException("game $gameId already has the max amount of players (2)!")
}
