package no.esa.battleship.repository.playershipcomponent

import QueryFileReader
import no.esa.battleship.repository.boardcoordinate.BoardCoordinateDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.ShipComponent
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
        const val TABLE_NAME = "player_ship_component"
        const val PRIMARY_KEY = "id"
        const val PLAYER_SHIP_ID = "player_ship_id"
        const val BOARD_COORDINATE_ID = "board_coordinate_id"
        const val IS_DESTROYED = "is_destroyed"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Synchronized
    override fun save(playerShipId: Int, coordinates: List<Coordinate>): List<ShipComponent> {
        return logger.log("playerShipId", playerShipId) {
            coordinates.map { coordinate ->
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

                val componentId = simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()

                ShipComponent(componentId, playerShipId, coordinate, false)
            }
        }
    }

    override fun findAllComponents(playerShipId: Int): List<ShipComponent> {
        val query = QueryFileReader.readSqlFile("/playershipcomponent/findAllComponents")
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            namedTemplate.query(query, parameterSource) { rs, _ ->
                val coordinateId = rs.getInt(BOARD_COORDINATE_ID)
                val xCoordinate = rs.getString(BoardCoordinateDao.X_COORDINATE)[0]
                val yCoordinate = rs.getInt(BoardCoordinateDao.Y_COORDINATE)

                ShipComponent(rs.getInt(PRIMARY_KEY),
                              rs.getInt(PLAYER_SHIP_ID),
                              Coordinate(coordinateId, xCoordinate, yCoordinate),
                              rs.getBoolean(IS_DESTROYED))
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
