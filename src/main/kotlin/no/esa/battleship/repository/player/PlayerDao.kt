package no.esa.battleship.repository.player

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Player
import no.esa.battleship.utils.classAndFunctionName
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction1

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
            try {
                simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
            } catch (error: Exception) {
                val message = "Could not save player: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not save player", this::class.java, error)
            }
        }
    }

    override fun findPlayersInGame(gameId: Int): List<Player> {
        val query = QueryFileReader.readSqlFile(::findPlayersInGame)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(GAME_ID, gameId)
        }

        return logger.log("gameId", gameId) {
            try {
                namedTemplate.query(query, parameterSource) { rs, _ ->
                    Player(rs.getInt(PRIMARY_KEY),
                           rs.getInt(GAME_ID))
                }
            } catch (error: Exception) {
                val message = "Could not find players in game: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not find players in game", this::class.java, error)
            }
        }
    }
}
