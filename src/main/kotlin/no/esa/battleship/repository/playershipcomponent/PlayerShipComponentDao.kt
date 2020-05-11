package no.esa.battleship.repository.playershipcomponent

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.boardcoordinate.CoordinateDao
import no.esa.battleship.repository.exceptions.DataAccessException
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
        const val COORDINATE_ID = "coordinate_id"
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
                    addValue(COORDINATE_ID, coordinate.id)
                    addValue(IS_DESTROYED, false)
                }

                val componentId = try {
                    simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
                } catch (error: Exception) {
                    throw DataAccessException("Failed to save player ship component",
                                              ::save,
                                              error)
                }

                ShipComponent(componentId, playerShipId, coordinate, false)
            }
        }
    }

    override fun findAllComponents(playerShipId: Int): List<ShipComponent> {
        val query = QueryFileReader.readSqlFile(::findAllComponents)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            try {
                namedTemplate.query(query, parameterSource) { rs, _ ->
                    val coordinateId = rs.getInt(COORDINATE_ID)
                    val xCoordinate = rs.getString(CoordinateDao.X_COORDINATE)[0]
                    val yCoordinate = rs.getInt(CoordinateDao.Y_COORDINATE)

                    ShipComponent(rs.getInt(PRIMARY_KEY),
                                  rs.getInt(PLAYER_SHIP_ID),
                                  Coordinate(coordinateId, xCoordinate, yCoordinate),
                                  rs.getBoolean(IS_DESTROYED))
                }
            } catch (error: Exception) {
                throw DataAccessException("Could not find ship components", ::findAllComponents, error)
            }
        }
    }

    @Synchronized
    override fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int {
        val query = QueryFileReader.readSqlFile(::update)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, playerShipComponentId)
        }

        return logger.log("playerShipComponentId", playerShipComponentId) {
            try {
                namedTemplate.update(query, parameterSource)
            } catch (error: Exception) {
                throw DataAccessException("Could not update ship components", ::update, error)
            }
        }
    }
}
