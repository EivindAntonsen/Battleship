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

