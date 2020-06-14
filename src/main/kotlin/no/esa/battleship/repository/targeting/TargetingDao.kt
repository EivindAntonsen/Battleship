package no.esa.battleship.repository.targeting

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.enums.TargetingMode
import no.esa.battleship.exceptions.NoSuchTargetingModeException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.TargetingEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class TargetingDao(private val jdbcTemplate: JdbcTemplate) : ITargetingDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "targeting"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val TARGET_PLAYER_ID = "target_player_id"
        const val TARGETING_MODE_ID = "targeting_mode_id"
    }

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Logged
    @DataAccess
    override fun find(playerId: Int): TargetingEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return namedTemplate.queryForObject(query, parameters) { rs, _ ->
            TargetingEntity(rs.getInt(PRIMARY_KEY),
                            rs.getInt(PLAYER_ID),
                            rs.getInt(TARGET_PLAYER_ID),
                            TargetingMode.fromInt(rs.getInt(TARGETING_MODE_ID)))
        } ?: throw NoSuchTargetingModeException()
    }

    @Synchronized
    @Logged
    @DataAccess
    override fun update(playerId: Int, targetingMode: TargetingMode): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::update)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_MODE_ID, targetingMode.id)
            addValue(PLAYER_ID, playerId)
        }

        return namedTemplate.update(query, parameters)
    }

    @Synchronized
    @Logged
    @DataAccess
    /**
     * Saves the initial targeting state for a given player. As it
     * happens before the first round, no ship will have been found,
     * hence it starts with TargetingMode.SEEK.
     */
    override fun save(playerId: Int,
                      targetPlayerId: Int,
                      gameTurnId: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(TARGET_PLAYER_ID, targetPlayerId)
            addValue(TARGETING_MODE_ID, TargetingMode.SEEK.id)
        }

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }
}
