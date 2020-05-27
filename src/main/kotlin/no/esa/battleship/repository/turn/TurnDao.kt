package no.esa.battleship.repository.turn

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.coordinate.CoordinateDao
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.TurnEntity
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class TurnDao(private val logger: Logger,
              private val jdbcTemplate: JdbcTemplate) : ITurnDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "turn"
        const val PRIMARY_KEY = "id"
        const val GAME_TURN = "game_turn"
        const val PLAYER_ID = "player_id"
        const val TARGET_PLAYER_ID = "target_player_id"
        const val COORDINATE_ID = "coordinate_id"
        const val IS_HIT = "is_hit"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(playerId: Int,
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
            addValue(PLAYER_ID, playerId)
            addValue(TARGET_PLAYER_ID, targetPlayerId)
            addValue(COORDINATE_ID, coordinateId)
            addValue(GAME_TURN, gameTurn)
            addValue(IS_HIT, isHit)
        }

        return logger.log {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun getPreviousTurnsForPlayer(playerId: Int): List<TurnEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getPreviousTurnsForPlayer)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    val coordinate = CoordinateEntity(rs.getInt(COORDINATE_ID),
                                                      rs.getString(CoordinateDao.X_COORDINATE)[0],
                                                      rs.getInt(CoordinateDao.Y_COORDINATE))

                    TurnEntity(rs.getInt(PRIMARY_KEY),
                               rs.getInt(GAME_TURN),
                               rs.getInt(PLAYER_ID),
                               rs.getInt(TARGET_PLAYER_ID),
                               coordinate,
                               rs.getBoolean(IS_HIT))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::getPreviousTurnsForPlayer, error)
            }
        }.sortedBy { it.gameTurn }
    }
}
