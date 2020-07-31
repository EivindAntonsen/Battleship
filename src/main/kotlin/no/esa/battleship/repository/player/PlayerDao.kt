package no.esa.battleship.repository.player

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.PlayerEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerDao(private val jdbcTemplate: JdbcTemplate) : IPlayerDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player"
        const val PRIMARY_KEY = "id"
        const val GAME_ID = "game_id"
        const val PLAYER_TYPE_ID = "player_type_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    @Logged
    @DataAccess
    override fun save(gameId: Int, playerTypeId: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            tableName = TABLE_NAME
            schemaName = SCHEMA_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
            addValue(PLAYER_TYPE_ID, playerTypeId)
        }

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @Logged
    @DataAccess
    override fun get(playerId: Int): PlayerEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, playerId)
        }

        return namedTemplate.queryForObject(query, parameters) { rs, _ ->
            PlayerEntity(rs.getInt(PRIMARY_KEY), rs.getInt(PLAYER_TYPE_ID), rs.getInt(GAME_ID))
        }!! //fixme
    }

    @Logged
    @DataAccess
    override fun getPlayersInGame(gameId: Int): List<PlayerEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getPlayersInGame)
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return namedTemplate.query(query, parameters) { rs, _ ->
            PlayerEntity(rs.getInt(PRIMARY_KEY),
                         rs.getInt(PLAYER_TYPE_ID),
                         rs.getInt(GAME_ID))
        }
    }
}
