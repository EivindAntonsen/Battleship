package no.esa.battleship.repository.exceptions

import kotlin.reflect.KFunction

data class DataAccessException(val callingFunction: KFunction<*>, override val cause: Throwable?) : RuntimeException()
