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

fun String.firstCharOrNull(): Char? = if (this.isEmpty()) null else this[0]
fun String.firstChar(): Char = this[0]

infix fun CoordinateEntity.isVerticallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.horizontalPosition == that.horizontalPosition
}

infix fun CoordinateEntity.isHorizontallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.verticalPosition == that.verticalPosition
}

infix fun CoordinateEntity.isAdjacentWith(that: CoordinateEntity): Boolean {
    val theyAreHorizontallyAligned = this isHorizontallyAlignedWith that
    val theyAreVerticallyAligned = this isVerticallyAlignedWith that
    val distanceHorizontallyApartIsOne = this.horizontalPositionAsInt() - that.horizontalPositionAsInt() in listOf(-1, 1)
    val distanceVerticallyApartIsOne = this.verticalPosition - that.verticalPosition in listOf(-1, 1)


    return theyAreHorizontallyAligned && distanceHorizontallyApartIsOne ||
            theyAreVerticallyAligned && distanceVerticallyApartIsOne
}

fun <T, R> Iterable<T>.flatMapIndexedNotNull(function: (index: Int, T) -> Iterable<R>?): List<R> {
    return flatMapIndexedTo(ArrayList(), function)
}

private inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapIndexedTo(destination: C,
                                                                                    transform: (index: Int, T) -> Iterable<R>?): C {

    forEachIndexed { index, element ->
        transform(index, element)?.let { elements ->
            destination.addAll(elements)
        }
    }

    return destination
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
