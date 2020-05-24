package no.esa.battleship.exceptions

class NoSuchShipStatusException(id: Int): RuntimeException("No ship status for id $id!")
