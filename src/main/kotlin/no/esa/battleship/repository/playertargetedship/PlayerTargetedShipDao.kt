package no.esa.battleship.repository.playertargetedship

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.PlayerTargetedShip
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerTargetedShipDao(private val logger: Logger,
                            private val jdbcTemplate: JdbcTemplate) : IPlayerTargetedShipDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_targeted_ship"
        const val PRIMARY_KEY = "id"
        const val PLAYER_TARGETING_ID = "player_targeting_id"
        const val PLAYER_SHIP_ID = "player_ship_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun save(playerTargetingId: Int, playerShipId: Int): Int {
        val jdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
            addValue(PLAYER_TARGETING_ID, playerTargetingId)
        }

        return logger.log("targetingId", playerTargetingId) {
            try {
                jdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    override fun findByTargetingId(playerTargetingId: Int): List<PlayerTargetedShip> {
        val query = QueryFileReader.readSqlFile(this::class, ::findByTargetingId)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_TARGETING_ID, playerTargetingId)
        }

        return logger.log("playerTargetingId", playerTargetingId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    PlayerTargetedShip(rs.getInt(PRIMARY_KEY),
                                       playerTargetingId,
                                       rs.getInt(PLAYER_SHIP_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findByTargetingId, error)
            }
        }
    }
}
