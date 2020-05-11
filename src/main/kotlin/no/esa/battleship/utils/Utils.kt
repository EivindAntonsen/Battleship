package no.esa.battleship.utils

import no.esa.battleship.service.domain.Coordinate
import org.slf4j.Logger
import kotlin.reflect.KClass

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

fun <R> Logger.log(identifier: String? = null, value: Any? = null, function: () -> R): R {
    val className = function::class.java.enclosingClass.simpleName
    val functionName = function::class.java.enclosingMethod.name

    if (identifier == null && value == null) {
        info("Call    \t${className}\t$functionName")
    } else info("Call    \t${className}\t$functionName\t($identifier = ${value.toString().abbreviate()})")

    val (response, duration) = executeAndMeasureTimeMillis(function)
    info("Response\t${response.toString().abbreviate()}\tin ${duration}ms.")

    return response
}

infix fun Coordinate.isHorizontallyAlignedWith(that: Coordinate): Boolean {
    return this.horizontal_position == that.horizontal_position
}

infix fun Coordinate.isVerticallyAlignedWith(that: Coordinate): Boolean {
    return this.vertical_position == that.vertical_position
}

infix fun Coordinate.isAdjacentWith(that: Coordinate): Boolean {
    return when {
        this isHorizontallyAlignedWith that -> {
            this.vertical_position - that.vertical_position in listOf(-1, 0, 1)

        }
        this isVerticallyAlignedWith that -> {
            this.horizontal_position - that.horizontal_position in listOf(-1, 0, 1)
        }
        else -> false
    }
}

fun classAndFunctionName(kClass: KClass<*>): Pair<String, String> {
    return kClass.java.enclosingClass.simpleName to kClass.java.enclosingMethod.name
}
