package no.esa.battleship.utils

inline fun <R> executeAndMeasureTimeMillis(function: () -> R): Pair<R, Long> {
    val startTime = System.currentTimeMillis()
    val result = function()

    return result to (System.currentTimeMillis() - startTime)
}
