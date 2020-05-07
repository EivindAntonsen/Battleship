package no.esa.battleship.repository.playership

import QueryFileReader
import no.esa.battleship.repository.model.Ship
import no.esa.battleship.exceptions.NoSuchShipException
import no.esa.battleship.repository.ShipMapper
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PlayerShipDao(private val logger: Logger,
                    private val jdbcTemplate: JdbcTemplate) : IPlayerShipDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val SHIP_TYPE_ID = "ship_type_id"
    }

    override fun find(id: Int): Ship {
        val query = QueryFileReader.readSqlFile("/playership/getShip.sql")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, id)
        }
        return logger.log("playerShipId", id) {
            namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                val playerId = rs.getInt(PLAYER_ID)
                val shipTypeId = rs.getInt(SHIP_TYPE_ID)

                ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
            } ?: throw NoSuchShipException(id)
        }
    }

    override fun save(ship: Ship): Int {
        TODO("Not yet implemented")
    }
}
