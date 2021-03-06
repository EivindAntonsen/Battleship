package no.esa.battleship.repository.turn

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.coordinate.CoordinateDao
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TurnEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class TurnDao(private val jdbcTemplate: JdbcTemplate) : ITurnDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "turn"
        const val PRIMARY_KEY = "id"
        const val GAME_ID = "game_id"
        const val GAME_TURN = "game_turn"
        const val PLAYER_ID = "player_id"
        const val TARGET_PLAYER_ID = "target_player_id"
        const val COORDINATE_ID = "coordinate_id"
        const val IS_HIT = "is_hit"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    @Logged
    @DataAccess
    override fun save(gameId: Int,
                      playerId: Int,
                      targetPlayerId: Int,
                      coordinateId: Int,
                      isHit: Boolean,
                      gameTurn: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
            addValue(PLAYER_ID, playerId)
            addValue(TARGET_PLAYER_ID, targetPlayerId)
            addValue(COORDINATE_ID, coordinateId)
            addValue(GAME_TURN, gameTurn)
            addValue(IS_HIT, isHit)
        }

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @DataAccess
    override fun getPreviousTurnsByPlayerId(playerId: Int): List<TurnEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getPreviousTurnsByPlayerId)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return getPreviousTurns(query, parameters)
    }

    @DataAccess
    override fun getPreviousTurnsByGameId(gameId: Int): List<TurnEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getPreviousTurnsByGameId)
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return getPreviousTurns(query, parameters)
    }

    @Logged
    private fun getPreviousTurns(query: String, parameters: MapSqlParameterSource): List<TurnEntity> {
        return namedTemplate.query(query, parameters) { rs, _ ->
            val coordinate = CoordinateEntity(rs.getInt(COORDINATE_ID),
                                              rs.getString(CoordinateDao.X_COORDINATE)[0],
                                              rs.getInt(CoordinateDao.Y_COORDINATE))

            TurnEntity(rs.getInt(PRIMARY_KEY),
                       rs.getInt(GAME_ID),
                       rs.getInt(GAME_TURN),
                       rs.getInt(PLAYER_ID),
                       rs.getInt(TARGET_PLAYER_ID),
                       coordinate,
                       rs.getBoolean(IS_HIT))
        }.sortedBy { it.id }
    }
}
