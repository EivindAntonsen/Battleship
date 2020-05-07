package no.esa.battleship.game

interface IPlayer {

    fun receiveShot(coordinate: Coordinate): Boolean
    fun shoot(): Coordinate
}
