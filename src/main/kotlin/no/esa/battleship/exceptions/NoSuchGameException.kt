package no.esa.battleship.exceptions

class NoSuchGameException(gameId: Int) : RuntimeException("No game found for id $gameId!")
