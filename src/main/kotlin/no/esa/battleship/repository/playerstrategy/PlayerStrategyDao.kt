package no.esa.battleship.repository.playerstrategy

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.enums.Strategy
import no.esa.battleship.exceptions.NoSuchStrategyException
import no.esa.battleship.repository.QueryFileReader
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerStrategyDao(private val jdbcTemplate: JdbcTemplate) : IPlayerStrategyDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_strategy"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val STRATEGY_ID = "strategy_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    @Logged
    @DataAccess
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

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @Logged
    @DataAccess
    override fun find(playerId: Int): Strategy {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return namedTemplate.queryForObject(query, parameters) { rs, _ ->
            Strategy.fromInt(rs.getInt(STRATEGY_ID))
        } ?: throw NoSuchStrategyException(this::class,
                                           ::find,
                                           "No strategy found for player with id $playerId!")
    }
}
