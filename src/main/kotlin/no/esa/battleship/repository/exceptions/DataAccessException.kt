package no.esa.battleship.repository.exceptions

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

data class DataAccessException(val callingClass: KClass<*>,
                               val callingFunction: KFunction<*>,
                               override val cause: Throwable?) : RuntimeException()
