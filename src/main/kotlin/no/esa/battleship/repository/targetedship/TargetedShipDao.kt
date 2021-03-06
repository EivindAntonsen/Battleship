package no.esa.battleship.repository.targetedship

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.TargetedShipEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class TargetedShipDao(private val jdbcTemplate: JdbcTemplate) : ITargetedShipDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "targeted_ship"
        const val PRIMARY_KEY = "id"
        const val TARGETING_ID = "targeting_id"
        const val SHIP_ID = "ship_id"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    @Logged
    @DataAccess
    override fun save(targetingId: Int, shipId: Int): Int {
        val jdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(SHIP_ID, shipId)
            addValue(TARGETING_ID, targetingId)
        }

        return jdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @Synchronized
    @Logged
    @DataAccess
    override fun delete(targetingId: Int, shipId: Int): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::delete)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_ID, targetingId)
            addValue(SHIP_ID, shipId)
        }

        return namedTemplate.update(query, parameters)
    }

    @Logged
    @DataAccess
    override fun getByTargetingId(targetingId: Int): List<TargetedShipEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getByTargetingId)
        val parameters = MapSqlParameterSource().apply {
            addValue(TARGETING_ID, targetingId)
        }

        return namedTemplate.query(query, parameters) { rs, _ ->
            TargetedShipEntity(rs.getInt(PRIMARY_KEY),
                               targetingId,
                               rs.getInt(SHIP_ID))
        }
    }
}
