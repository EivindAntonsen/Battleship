package no.esa.battleship.repository.game

import no.esa.battleship.exceptions.NoSuchGameException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.entity.GameEntity
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Repository
class GameDao(private val logger: Logger,
              private val jdbcTemplate: JdbcTemplate) : IGameDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val DATETIME = "datetime"
        const val GAME_SERIES_ID = "game_series_id"
        const val IS_CONCLUDED = "is_concluded"
    }

    override fun get(gameId: Int): GameEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return logger.log("gameId", gameId) {
            try {
                namedTemplate.queryForObject(query, parameters) { rs: ResultSet, _ ->
                    val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()
                    val isConcluded = rs.getBoolean(IS_CONCLUDED)
                    val gameSeriesId = rs.getString(GAME_SERIES_ID)?.let {
                        UUID.fromString(it)
                    }

                    GameEntity(gameId, dateTime, gameSeriesId, isConcluded)
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::get, error)
            }
        } ?: throw NoSuchGameException(gameId)
    }

    override fun isGameConcluded(gameId: Int): Boolean = get(gameId).isConcluded

    @Synchronized
    override fun conclude(gameId: Int): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::conclude)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return logger.log(PRIMARY_KEY, gameId) {
            try {
                namedTemplate.update(query, parameters)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::conclude, error)
            }
        }
    }

    @Synchronized
    override fun save(datetime: LocalDateTime): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameters = MapSqlParameterSource().apply {
            addValue(DATETIME, datetime)
            addValue(IS_CONCLUDED, false)
        }

        return logger.log {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun findGamesInSeries(gameSeriesId: UUID): List<GameEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::findGamesInSeries)
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_SERIES_ID, gameSeriesId)
        }

        return logger.log("gameSeriesId", gameSeriesId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    val gameId = rs.getInt(PRIMARY_KEY)
                    val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()
                    val isConcluded = rs.getBoolean(IS_CONCLUDED)

                    GameEntity(gameId, dateTime, gameSeriesId, isConcluded)
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findGamesInSeries, error)
            }
        }
    }
}
