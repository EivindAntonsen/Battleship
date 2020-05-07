package no.esa.battleship.repository.playershipcomponent

import QueryFileReader
import no.esa.battleship.repository.model.Coordinate
import no.esa.battleship.repository.model.ShipComponent
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerShipComponentDao(private val logger: Logger,
                             private val jdbcTemplate: JdbcTemplate) : IPlayerShipComponentDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val PLAYER_SHIP_ID = "player_ship_id"
        const val BOARD_COORDINATE_ID = "board_coordinate_id"
        const val IS_DESTROYED = "is_destroyed"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(playerShipId: Int, coordinate: Coordinate): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
            addValue(BOARD_COORDINATE_ID, coordinate.id)
            addValue(IS_DESTROYED, false)
        }

        return logger.log("playerShipId", playerShipId) {
            simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
        }
    }

    override fun findAllComponents(playerShipId: Int): List<ShipComponent> {
        val query = QueryFileReader.readSqlFile("/playershipcomponent/getAllComponents")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            namedTemplate.query(query, parameterSource) { rs, _ ->
                val coordinateId = rs.getInt(3)
                val xCoordinate = rs.getString(4)[0]
                val yCoordinate = rs.getInt(5)

                ShipComponent(rs.getInt(1),
                        rs.getInt(2),
                        Coordinate(coordinateId, xCoordinate, yCoordinate),
                        rs.getBoolean(6))
            }
        }
    }

    @Synchronized
    override fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int {
        val query = QueryFileReader.readSqlFile("/playershipcomponent/update")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, playerShipComponentId)
        }

        return logger.log("playerShipComponentId", playerShipComponentId) {
            namedTemplate.update(query, parameterSource)
        }
    }
}
