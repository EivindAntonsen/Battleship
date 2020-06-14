package no.esa.battleship.exceptions

import kotlin.reflect.KClass
import kotlin.reflect.KFunction

abstract class GameStateException : RuntimeException() {
    abstract val callingClass: KClass<*>
    abstract val callingFunction: KFunction<*>
    abstract override val message: String?
    abstract override val cause: Throwable?
}
