package no.esa.battleship.exceptions

sealed class ShipConfigurationException : RuntimeException() {
    class NoCoordinatesFoundException(override val message: String) : ShipConfigurationException()
    class InvalidAlignmentException(override val message: String) : ShipConfigurationException()
}
