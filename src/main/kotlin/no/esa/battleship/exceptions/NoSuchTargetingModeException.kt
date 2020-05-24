package no.esa.battleship.exceptions

class NoSuchTargetingModeException(id: Int) : RuntimeException("No targeting mode for id $id!")
