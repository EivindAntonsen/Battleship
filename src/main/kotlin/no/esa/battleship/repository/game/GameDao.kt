package no.esa.battleship.repository.game

import no.esa.battleship.exceptions.NoSuchGameException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Game
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GameDao(private val logger: Logger,
              private val jdbcTemplate: JdbcTemplate) : IGameDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val DATETIME = "datetime"
        const val IS_CONCLUDED = "is_concluded"
    }

    override fun get(gameId: Int): Game {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return logger.log("gameId", gameId) {
            try {
                namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                    val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()
                    val isConcluded = rs.getBoolean(IS_CONCLUDED)

                    Game(gameId, dateTime, isConcluded)
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::get, error)
            }
        } ?: throw NoSuchGameException(gameId)
    }

    override fun isGameConcluded(gameId: Int): Boolean = get(gameId).isConcluded

    @Synchronized
    override fun conclude(gameId: Int): Int {
        val statement = QueryFileReader.readSqlFile(this::class, ::conclude)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return logger.log(PRIMARY_KEY, gameId) {
            try {
                namedTemplate.update(statement, parameterSource)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::conclude, error)
            }
        }
    }

    @Synchronized
    override fun save(datetime: LocalDateTime): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameterSource = MapSqlParameterSource().apply {
            addValue(DATETIME, datetime)
            addValue(IS_CONCLUDED, false)
        }

        return logger.log {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }
}
