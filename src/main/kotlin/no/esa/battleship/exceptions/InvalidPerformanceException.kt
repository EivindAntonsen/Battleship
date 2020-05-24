package no.esa.battleship.exceptions

class InvalidPerformanceException(playerId: Int) : RuntimeException("Impossible game state, no shots were fired for player $playerId!")
