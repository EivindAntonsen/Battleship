package no.esa.battleship.repository.game

import no.esa.battleship.game.Game
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class GameDao(private val logger: Logger,
              private val jdbcTemplate: JdbcTemplate) : IGameDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        private const val SCHEMA_NAME = "battleship"
        private const val TABLE_NAME = "game"
        private const val PRIMARY_KEY = "id"
        private const val DATETIME = "datetime"
        private const val IS_CONCLUDED = "is_concluded"
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
