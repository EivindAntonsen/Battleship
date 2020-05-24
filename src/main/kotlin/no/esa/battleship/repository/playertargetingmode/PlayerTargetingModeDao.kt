package no.esa.battleship.repository.playertargetingmode

import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerTargetingModeDao(private val logger: Logger,
                             private val jdbcTemplate: JdbcTemplate) : IPlayerTargetingModeDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_targeting_mode"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val TARGETING_MODE_ID = "targeting_mode_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun find(playerId: Int): TargetingMode {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.queryForObject(query, parameters) { rs, _ ->
                    TargetingMode.fromInt(rs.getInt(PRIMARY_KEY))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::find, error)
            }
        }
    }

    @Synchronized
    override fun update(playerId: Int, targetingMode: TargetingMode): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::update)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_MODE_ID, targetingMode.id)
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.update(query, parameters)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::update, error)
            }
        }
    }

    @Synchronized
    override fun save(playerId: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(TARGETING_MODE_ID, TargetingMode.SEEK.id)
        }

        return logger.log("playerId", playerId) {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }
}
