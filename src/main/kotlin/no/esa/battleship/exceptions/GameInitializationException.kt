package no.esa.battleship.exceptions

sealed class GameInitializationException(message: String) : RuntimeException(message) {
    class ShipPlacementException(message: String) : GameInitializationException(message)
}
