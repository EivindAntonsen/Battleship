package no.esa.battleship.repository

import no.esa.battleship.repository.exceptions.QueryFileNotFoundException
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction

object QueryFileReader {

    private val logger = LoggerFactory.getLogger("no.esa.battleship.repository.QueryFileReader")

    fun readSqlFile(packageAndFilePath: String): String {
        return try {
            this::class.java.getResource("/db/queries/$packageAndFilePath.sql").readText()
        } catch (error: Exception) {
            val message = "Attempt to read from /db/queries/$packageAndFilePath failed: ${error.message}."
            logger.error(message)

            throw QueryFileNotFoundException(message)
        }
    }

    fun <R> readSqlFile(f: KFunction<R>): String {
        val classAndFunction = "${f.javaClass.enclosingClass.simpleName.toLowerCase()}/${f.name}"

        return try {
            this::class.java.getResource("/db/queries/$classAndFunction.sql").readText()
        } catch (error: Exception) {
            val message = "Attempt to read from /db/queries/$classAndFunction.sql failed: ${error.message}."
            logger.error(message)

            throw QueryFileNotFoundException(message)
        }
    }
}
