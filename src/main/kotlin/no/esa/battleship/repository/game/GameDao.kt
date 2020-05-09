package no.esa.battleship.repository.game

import QueryFileReader
import no.esa.battleship.exceptions.NoSuchGameException
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
        val query = QueryFileReader.readSqlFile("game/findGame")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return logger.log("gameId", gameId) {
            namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()

                Game(gameId, dateTime)
            } ?: throw NoSuchGameException(gameId)
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
            simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
        }
    }
}
