package no.esa.battleship.utils

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

@Deprecated("Use zipWithNext instead.")
inline fun <S, T : S> Iterable<T>.validatedFold(validationFunction: (acc: S, T) -> Boolean): Boolean {
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

