package no.esa.battleship.repository.game

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.exceptions.NoSuchGameException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.GameEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.LocalDateTime
import java.util.*

@Repository
class GameDao(private val jdbcTemplate: JdbcTemplate) : IGameDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val DATETIME = "datetime"
        const val GAME_SERIES_ID = "game_series_id"
        const val IS_CONCLUDED = "is_concluded"
    }

    @Logged
    @DataAccess
    override fun get(gameId: Int): GameEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::get)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return namedTemplate.queryForObject(query, parameters) { rs: ResultSet, _ ->
            val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()
            val isConcluded = rs.getBoolean(IS_CONCLUDED)
            val gameSeriesId = rs.getString(GAME_SERIES_ID)?.let {
                UUID.fromString(it)
            }

            GameEntity(gameId, dateTime, isConcluded)
        } ?: throw NoSuchGameException(this::class, ::get, "No game found with id $gameId!")
    }

    override fun isGameConcluded(gameId: Int): Boolean = get(gameId).isConcluded

    @Synchronized
    @Logged
    @DataAccess
    override fun conclude(gameId: Int): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::conclude)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, gameId)
        }

        return namedTemplate.update(query, parameters)
    }

    @Synchronized
    @Logged
    @DataAccess
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

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @Logged
    @DataAccess
    override fun getGamesInSeries(gameSeriesId: UUID): List<GameEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getGamesInSeries)
        val parameters = MapSqlParameterSource().apply {
            addValue(GAME_SERIES_ID, gameSeriesId)
        }

        return namedTemplate.query(query, parameters) { rs, _ ->
            val gameId = rs.getInt(PRIMARY_KEY)
            val dateTime = rs.getTimestamp(DATETIME).toLocalDateTime()
            val isConcluded = rs.getBoolean(IS_CONCLUDED)

            GameEntity(gameId, dateTime, isConcluded)
        }
    }
}
