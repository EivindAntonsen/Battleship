package no.esa.battleship.repository.targetedship

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.TargetedShipEntity
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class TargetedShipDao(private val logger: Logger,
                      private val jdbcTemplate: JdbcTemplate) : ITargetedShipDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "targeted_ship"
        const val PRIMARY_KEY = "id"
        const val TARGETING_ID = "targeting_id"
        const val SHIP_ID = "ship_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun save(playerTargetingId: Int, playerShipId: Int): Int {
        val jdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(SHIP_ID, playerShipId)
            addValue(TARGETING_ID, playerTargetingId)
        }

        return logger.log("targetingId", playerTargetingId) {
            try {
                jdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    @Synchronized
    override fun delete(targetingId: Int, shipId: Int): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::delete)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_ID, targetingId)
            addValue(SHIP_ID, shipId)
        }

        return logger.log("targetingId", targetingId) {
            try {
                namedTemplate.update(query, parameters)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::delete, error)
            }
        }
    }

    override fun findByTargetingId(playerTargetingId: Int): List<TargetedShipEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::findByTargetingId)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_ID, playerTargetingId)
        }

        return logger.log("playerTargetingId", playerTargetingId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    TargetedShipEntity(rs.getInt(PRIMARY_KEY),
                                       playerTargetingId,
                                       rs.getInt(SHIP_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findByTargetingId, error)
            }
        }
    }
}
