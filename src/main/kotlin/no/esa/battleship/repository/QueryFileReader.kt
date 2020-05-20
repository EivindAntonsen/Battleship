package no.esa.battleship.repository

import no.esa.battleship.repository.exceptions.QueryFileNotFoundException
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

object QueryFileReader {

    private val logger = LoggerFactory.getLogger("no.esa.battleship.repository.QueryFileReader")

    fun readSqlFile(callingClass: KClass<*>, callingFunction: KFunction<*>): String {
        val path = "/db/queries/${callingClass.simpleName}/${callingFunction.name}.sql"

        return try {
            this::class.java.getResource(path).readText()
        } catch (error: Exception) {
            val message = "Attempt to read from $path failed: ${error.message}."
            logger.error(message)

            throw QueryFileNotFoundException(message)
        }
    }
}
