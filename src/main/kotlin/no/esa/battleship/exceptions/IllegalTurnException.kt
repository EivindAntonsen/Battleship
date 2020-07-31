package no.esa.battleship.exceptions

class IllegalTurnException(coordinateId: Int) : RuntimeException("Coordinate with id $coordinateId is not a valid coordinate!")
