package no.esa.battleship.repository.result

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Result
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class ResultDao(private val logger: Logger,
                private val jdbcTemplate: JdbcTemplate) : IResultDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "result"
        const val PRIMARY_KEY = "id"
        const val GAME_ID = "game_id"
        const val WINNING_PLAYER_ID = "winning_player_id"
    }

    private val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(gameId: Int, winningPlayerId: Int?): Result {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
            winningPlayerId?.let {
                addValue(WINNING_PLAYER_ID, it)
            }
        }

        return logger.log("gameId", gameId) {
            try {
                val id = simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()

                Result(id, gameId, winningPlayerId)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun get(gameId: Int): Result {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return logger.log("gameId", gameId) {
            try {
                namedParameterJdbcTemplate.queryForObject(query, parameterSource) { rs, _ ->
                    Result(rs.getInt(PRIMARY_KEY),
                           rs.getInt(GAME_ID),
                           rs.getInt(WINNING_PLAYER_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::get, error)
            }
        }
    }

    override fun getAll() {
        val query = QueryFileReader.readSqlFile(this::class, ::getAll)

        return logger.log {
            try {
                jdbcTemplate.query(query) { rs, _ ->
                    Result(rs.getInt(PRIMARY_KEY),
                           rs.getInt(GAME_ID),
                           rs.getInt(WINNING_PLAYER_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::getAll, error)
            }
        }
    }
}
