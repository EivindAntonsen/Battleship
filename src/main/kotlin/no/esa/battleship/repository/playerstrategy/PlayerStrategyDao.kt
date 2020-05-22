package no.esa.battleship.repository.playerstrategy

import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.NoSuchStrategyException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerStrategyDao(private val logger: Logger,
                        private val jdbcTemplate: JdbcTemplate) : IPlayerStrategyDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_strategy"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val STRATEGY_ID = "strategy_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(playerId: Int, strategy: Strategy): Int {
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(STRATEGY_ID, strategy.id)
        }
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        return logger.log("playerId", playerId) {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun find(playerId: Int): Strategy {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.queryForObject(query, parameters) { rs, _ ->
                    Strategy.fromInt(rs.getInt(STRATEGY_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::find, error)
            } ?: throw NoSuchStrategyException(playerId)
        }
    }
}
