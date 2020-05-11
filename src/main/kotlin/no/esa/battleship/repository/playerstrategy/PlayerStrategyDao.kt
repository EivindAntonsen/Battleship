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
        val parameterSource = MapSqlParameterSource().apply {
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
                simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
            } catch (error: Exception) {
                val message = "Could not save player strategy: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not save player strategy",
                                          this::class.java,
                                          error)
            }
        }
    }

    override fun find(playerId: Int): Strategy {
        val query = QueryFileReader.readSqlFile(::find)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                    Strategy.fromInt(rs.getInt(STRATEGY_ID))
                } ?: throw NoSuchStrategyException(playerId)
            } catch (error: Exception) {
                val message = "Could not get player strategy: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not get player strategy",
                                          this::class.java,
                                          error)
            }
        }
    }
}
