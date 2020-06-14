package no.esa.battleship.repository.result

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.ResultEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class ResultDao(private val jdbcTemplate: JdbcTemplate) : IResultDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "result"
        const val PRIMARY_KEY = "id"
        const val GAME_ID = "game_id"
        const val WINNING_PLAYER_ID = "winning_player_id"
    }

    private val namedParameterJdbcTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    @Logged
    @DataAccess
    override fun save(gameId: Int, winningPlayerId: Int?): ResultEntity {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
            winningPlayerId?.let {
                addValue(WINNING_PLAYER_ID, it)
            }
        }

        val id = simpleJdbcInsert.executeAndReturnKey(parameters).toInt()

        return ResultEntity(id, gameId, winningPlayerId)
    }

    @Logged
    @DataAccess
    override fun get(gameId: Int): ResultEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return namedParameterJdbcTemplate.queryForObject(query, parameters) { rs, _ ->
            ResultEntity(rs.getInt(PRIMARY_KEY),
                         rs.getInt(GAME_ID),
                         rs.getInt(WINNING_PLAYER_ID))
        }!! //fixme
    }

    @Logged
    @DataAccess
    override fun getAll(): List<ResultEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getAll)

        return jdbcTemplate.query(query) { rs, _ ->
            ResultEntity(rs.getInt(PRIMARY_KEY),
                         rs.getInt(GAME_ID),
                         rs.getInt(WINNING_PLAYER_ID))
        }
    }
}
