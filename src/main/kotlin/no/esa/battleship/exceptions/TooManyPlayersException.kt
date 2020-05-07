package no.esa.battleship.exceptions

class TooManyPlayersException(gameId: Int) : RuntimeException("game $gameId already has the max amount of players (2)!")
