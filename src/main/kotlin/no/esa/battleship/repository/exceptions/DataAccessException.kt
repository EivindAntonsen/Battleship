package no.esa.battleship.repository.exceptions

import kotlin.reflect.KFunction

data class DataAccessException(override val message: String,
                               val function: KFunction<*>,
                               override val cause: Throwable?) : RuntimeException(message)
