package no.esa.battleship.repository.player

import QueryFileReader
import no.esa.battleship.service.domain.Player
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerDao(private val logger: Logger,
                private val jdbcTemplate: JdbcTemplate) : IPlayerDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player"
        const val PRIMARY_KEY = "id"
        const val GAME_ID = "game_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(gameId: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            tableName = TABLE_NAME
            schemaName = SCHEMA_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return logger.log {
            simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
        }
    }

    override fun findPlayersInGame(gameId: Int): List<Player> {
        val query = QueryFileReader.readSqlFile("/player/findPlayersByGameId")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return logger.log("gameId", gameId) {
            namedTemplate.query(query, parameterSource) { rs, _ ->
                Player(rs.getInt(PRIMARY_KEY),
                       rs.getInt(GAME_ID))
            }
        }
    }
}
