package no.esa.battleship.repository.entity

data class CoordinateEntity(val id: Int,
                            val horizontalPosition: Char,
                            val verticalPosition: Int) {

    companion object {
        val HORIZONTAL_POSITION_TO_INT = ('a'..'j').mapIndexed { index, char ->
            char to index + 1
        }.toMap()
    }

    fun horizontalPositionAsInt(): Int {
        return HORIZONTAL_POSITION_TO_INT.getValue(horizontalPosition)
    }
}
