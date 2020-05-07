package no.esa.battleship.exceptions

class NoSuchShipTypeException(shipTypeId: Int) : RuntimeException("No ship type matching $shipTypeId!")
