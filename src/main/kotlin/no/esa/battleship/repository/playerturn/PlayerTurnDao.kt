package no.esa.battleship.repository.playerturn

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.boardcoordinate.CoordinateDao
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.PlayerTurn
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerTurnDao(private val logger: Logger,
                    private val jdbcTemplate: JdbcTemplate) : IPlayerTurnDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_turn"
        const val PRIMARY_KEY = "id"
        const val GAME_TURN = "game_turn"
        const val PLAYER_ID = "player_id"
        const val COORDINATE_ID = "coordinate_id"
        const val IS_HIT = "is_hit"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(playerId: Int,
                      coordinateId: Int,
                      isHit: Boolean,
                      gameTurn: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(COORDINATE_ID, coordinateId)
            addValue(GAME_TURN, gameTurn)
            addValue(IS_HIT, isHit)
        }

        return logger.log {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun getPreviousTurnsForPlayer(playerId: Int): List<PlayerTurn> {
        val query = QueryFileReader.readSqlFile(this::class, ::getPreviousTurnsForPlayer)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.query(query, parameterSource) { rs, _ ->
                    val coordinate = Coordinate(rs.getInt(CoordinateDao.PRIMARY_KEY),
                                                rs.getString(CoordinateDao.X_COORDINATE)[0],
                                                rs.getInt(CoordinateDao.Y_COORDINATE))

                    PlayerTurn(rs.getInt(PRIMARY_KEY),
                               rs.getInt(GAME_TURN),
                               rs.getInt(PLAYER_ID),
                               coordinate,
                               rs.getBoolean(IS_HIT))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::getPreviousTurnsForPlayer, error)
            }
        }.sortedBy { it.gameTurn }
    }
}
