package no.esa.battleship.utils

import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.service.domain.Coordinate
import org.slf4j.Logger

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

fun <R> Logger.log(identifier: String? = null,
                   value: Any? = null,
                   function: () -> R): R {
    val className = function::class.java.enclosingClass.simpleName
    val functionName = function::class.java.enclosingMethod.name

    val logMessage = "Call    \t${className}\t$functionName" + {
        if (identifier != null && value != null) {
            "\t($identifier = ${value.toString().abbreviate()})"
        } else null
    }

    info(logMessage)

    val (response, duration) = executeAndMeasureTimeMillis(function)

    info("Response\t${response.toString().abbreviate()}\tin ${duration}ms.")

    return response
}

infix fun CoordinateEntity.isVerticallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.horizontal_position == that.horizontal_position
}

infix fun Coordinate.isVerticallyAlignedWith(that: Coordinate): Boolean = this.x == that.x

infix fun Coordinate.isHorizontallyAlignedWith(that: Coordinate): Boolean = this.y == that.y

infix fun Coordinate.isAdjacentWith(that: Coordinate): Boolean {
    return (this isHorizontallyAlignedWith that && this.xAsInt() - that.xAsInt() in listOf(-1, 1)) ||
            (this isVerticallyAlignedWith that && this.y - that.y in listOf(-1, 1))
}

infix fun CoordinateEntity.isHorizontallyAlignedWith(that: CoordinateEntity): Boolean {
    return this.vertical_position == that.vertical_position
}

infix fun CoordinateEntity.isAdjacentWith(that: CoordinateEntity): Boolean {
    return (this isHorizontallyAlignedWith that &&
            this.horizontalPositionAsInt() - that.horizontalPositionAsInt() in listOf(-1, 1)) ||
            (this isVerticallyAlignedWith that && this.vertical_position - that.vertical_position in listOf(-1, 1))
}

fun String.toCamelCase(): String? {
    return try {
        this[0].toLowerCase() + substring(1)
    } catch (error: Exception) {
        null
    }
}
