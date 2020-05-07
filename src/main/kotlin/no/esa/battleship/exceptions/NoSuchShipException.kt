package no.esa.battleship.exceptions

class NoSuchShipException(playerShipId: Int) : RuntimeException("Found no player ship matching id $playerShipId!")
