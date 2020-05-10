package no.esa.battleship.exceptions

class NoSuchStrategyException(playerId: Int) : RuntimeException("No strategy was found for player $playerId.")
