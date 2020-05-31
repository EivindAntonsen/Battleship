package no.esa.battleship.service.domain

import no.esa.battleship.exceptions.NoSuchCoordinateException

data class Coordinate(val x: Char, val y: Int) {

    companion object {
        val HORIZONTAL_POSITION_TO_INT = ('a'..'j').mapIndexed { index, char ->
            char to index + 1
        }.toMap()
    }

    fun xAsInt(): Int {
        return HORIZONTAL_POSITION_TO_INT[x] ?: throw NoSuchCoordinateException(x)
    }
}
