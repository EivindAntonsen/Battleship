package no.esa.battleship.exceptions

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class NoAvailableCoordinatesLeftException(val callingClass: KClass<*>,
                                          val callingFunction: KFunction<*>,
                                          override val message: String) : RuntimeException() {
}
