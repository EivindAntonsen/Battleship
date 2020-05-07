package no.esa.battleship.repository.player

import no.esa.battleship.game.Player
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class PlayerDao(private val logger: Logger,
                private val jdbcTemplate: JdbcTemplate) : IPlayerDao {

    companion object {
        private const val SCHEMA_NAME = "battleship"
        private const val TABLE_NAME = "game"
        private const val PRIMARY_KEY = "id"
        private const val GAME_ID = "game_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)


    override fun save(player: Player): Int {
        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, player.gameId)
        }

        return logger.log("playerId", player.id) {
            SimpleJdbcInsert(jdbcTemplate).apply {
                tableName = TABLE_NAME
                schemaName = SCHEMA_NAME
                usingGeneratedKeyColumns(PRIMARY_KEY)
            }.executeAndReturnKey(parameterSource).toInt()
        }
    }

    override fun find(uuid: UUID): Player? {
        TODO("Not yet implemented")
    }

    override fun findByGameId(gameId: Int): List<Player> {
        TODO("Not yet implemented")
    }

    override fun delete(uuid: UUID): Int {
        TODO("Not yet implemented")
    }

    override fun deleteByGameId(gameId: Int): Int {
        TODO("Not yet implemented")
    }
}
