package no.esa.battleship.game

data class Coordinate(val id: Int, val x: Char, val y: Int) {
    init {
        require(x.toLowerCase() in listOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'))
        require(y in 1..10)
    }
}
