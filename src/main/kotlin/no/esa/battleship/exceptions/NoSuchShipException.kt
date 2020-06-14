package no.esa.battleship.exceptions

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class NoSuchShipException(override val callingClass: KClass<*>,
                          override val callingFunction: KFunction<*>,
                          override val message: String? = null,
                          override val cause: Throwable? = null) : GameStateException()
