package no.esa.battleship.exceptions

class NoSuchPlayerTypeException(playerTypeId: Int) : RuntimeException("No player type matching $playerTypeId!")
