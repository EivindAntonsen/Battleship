package no.esa.battleship.utils

const val STRING_MAX_LENGTH = 100

fun String.abbreviate(): String {
    return if (this.length > STRING_MAX_LENGTH) {
        this.substring(0, STRING_MAX_LENGTH - 3).plus("...")
    } else this
}

fun String.firstCharOrNull(): Char? = if (this.isEmpty()) null else this[0]
fun String.firstChar(): Char = this[0]
