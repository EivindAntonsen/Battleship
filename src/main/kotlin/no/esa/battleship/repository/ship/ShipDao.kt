package no.esa.battleship.repository.ship

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.exceptions.NoSuchShipException
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.mapper.ShipMapper
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.entity.ShipEntity
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class ShipDao(private val jdbcTemplate: JdbcTemplate) : IShipDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "ship"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val SHIP_TYPE_ID = "ship_type_id"
    }

    @Logged
    @DataAccess
    override fun findAllShipsForPlayer(playerId: Int): List<ShipEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::findAllShipsForPlayer)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return namedTemplate.query(query, parameters) { rs, _ ->
            val shipTypeId = rs.getInt(SHIP_TYPE_ID)
            val id = rs.getInt(PRIMARY_KEY)

            ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
        }
    }

    @Logged
    @DataAccess
    override fun find(id: Int): ShipEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, id)
        }

        return namedTemplate.queryForObject(query, parameters) { rs, _ ->
            val playerId = rs.getInt(PLAYER_ID)
            val shipTypeId = rs.getInt(SHIP_TYPE_ID)

            ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
        } ?: throw NoSuchShipException(id)
    }

    @Synchronized
    @Logged
    @DataAccess
    override fun save(playerId: Int, shipTypeId: Int): ShipEntity {
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(SHIP_TYPE_ID, shipTypeId)
        }
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val shipId = simpleJdbcInsert.executeAndReturnKey(parameters).toInt()

        return ShipMapper.fromShipTypeIdWithParameters(shipId, playerId, shipTypeId)
    }
}
