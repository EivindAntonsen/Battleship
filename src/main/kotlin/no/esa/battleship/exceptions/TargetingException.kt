package no.esa.battleship.exceptions

sealed class TargetingException : RuntimeException() {
    class NoAvailableCoordinatesException(override val message: String) : TargetingException()
    class NotEnoughAvailableCoordinatesException(override val message: String) : TargetingException()
    class ScoringException(override val message: String) : TargetingException()
}
