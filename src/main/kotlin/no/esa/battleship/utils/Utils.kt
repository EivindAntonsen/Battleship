package no.esa.battleship.utils

import no.esa.battleship.repository.entity.CoordinateEntity

const val STRING_MAX_LENGTH = 100

inline fun <R> executeAndMeasureTimeMillis(function: () -> R): Pair<R, Long> {
    val startTime = System.currentTimeMillis()
    val result = function()

    return result to (System.currentTimeMillis() - startTime)
}

fun String.abbreviate(): String {
    return if (this.length > STRING_MAX_LENGTH) {
        this.substring(0, STRING_MAX_LENGTH - 3).plus("...")
    } else this
}

infix fun CoordinateEntity.isVerticallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.horizontal_position == that.horizontal_position
}

infix fun CoordinateEntity.isHorizontallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.vertical_position == that.vertical_position
}

infix fun CoordinateEntity.isAdjacentWith(that: CoordinateEntity): Boolean {
    return this isHorizontallyAlignedWith that &&
            this.horizontalPositionAsInt() - that.horizontalPositionAsInt() in listOf(-1, 1) ||
            this isVerticallyAlignedWith that &&
            this.vertical_position - that.vertical_position in listOf(-1, 1)
}

fun <S, T : S> Iterable<T>.validateElements(validationFunction: (acc: S, T) -> Boolean): Boolean {
    val iterator = this.iterator()
    if (!iterator.hasNext()) return false

    var accumulator: T = iterator.next()
    var isValid = true

    while (iterator.hasNext()) {
        val a = iterator.next()
        val elementsAreValid = validationFunction(accumulator, a)

        if (elementsAreValid) {
            accumulator = a
        } else {
            isValid = false
            break
        }
    }

    return isValid
}
