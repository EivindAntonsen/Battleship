package no.esa.battleship.repository.playerboard

import QueryFileReader
import no.esa.battleship.repository.model.PlayerBoard
import no.esa.battleship.exceptions.InvalidGameStateException
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PlayerBoardDao(private val logger: Logger,
                     private val jdbcTemplate: JdbcTemplate) : IPlayerBoardDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun getBoard(playerId: Int): PlayerBoard {
        val query = QueryFileReader.readSqlFile("/playerboard/getBoard")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                PlayerBoard(rs.getInt(PRIMARY_KEY), rs.getInt(PLAYER_ID))
            } ?: throw InvalidGameStateException("Found no game board for player $playerId!")
        }
    }
}
