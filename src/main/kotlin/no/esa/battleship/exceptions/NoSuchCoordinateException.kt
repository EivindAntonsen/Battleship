package no.esa.battleship.exceptions

class NoSuchCoordinateException(char: Char) : RuntimeException("No integer value found for horizontal position $char!")
