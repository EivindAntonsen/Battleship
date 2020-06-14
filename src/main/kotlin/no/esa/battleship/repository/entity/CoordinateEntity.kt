package no.esa.battleship.repository.entity

import no.esa.battleship.exceptions.NoSuchCoordinateException

data class CoordinateEntity(val id: Int,
                            val horizontal_position: Char,
                            val vertical_position: Int) {

    companion object {
        val HORIZONTAL_POSITION_TO_INT = ('a'..'j').mapIndexed { index, char ->
            char to index + 1
        }.toMap()
    }

    fun horizontalPositionAsInt(): Int {
        return HORIZONTAL_POSITION_TO_INT[horizontal_position] ?: throw NoSuchCoordinateException(horizontal_position)
    }
}
