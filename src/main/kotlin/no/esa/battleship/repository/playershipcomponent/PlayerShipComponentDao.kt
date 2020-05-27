package no.esa.battleship.repository.playershipcomponent

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.coordinate.CoordinateDao
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.player.PlayerDao
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.service.domain.Component
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
    override fun save(playerShipId: Int, coordinates: List<Coordinate>): List<Component> {
        return logger.log("playerShipId", playerShipId) {
            coordinates.map { coordinate ->
                val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
                    schemaName = SCHEMA_NAME
                    tableName = TABLE_NAME
                    usingGeneratedKeyColumns(PRIMARY_KEY)
                }

                val parameters = MapSqlParameterSource().apply {
                    addValue(PLAYER_SHIP_ID, playerShipId)
                    addValue(COORDINATE_ID, coordinate.id)
                    addValue(IS_DESTROYED, false)
                }

                val componentId = try {
                    simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
                } catch (error: Exception) {
                    throw DataAccessException(this::class, ::save, error)
                }

                Component(componentId, playerShipId, coordinate, false)
            }
        }
    }

    override fun findByGameId(gameId: Int): List<Component> {
        val query = QueryFileReader.readSqlFile(this::class, ::findByGameId)
        val parameters = MapSqlParameterSource().apply {
            addValue(PlayerDao.GAME_ID, gameId)
        }

        return logger.log("gameId", gameId) {
            find(query, parameters)
        }
    }

    override fun findByPlayerShipId(playerShipId: Int): List<Component> {
        val query = QueryFileReader.readSqlFile(this::class, ::findByPlayerShipId)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            find(query, parameters)
        }
    }

    fun find(query: String, parameterSource: MapSqlParameterSource): List<Component> {
        return try {
            namedTemplate.query(query, parameterSource) { rs, _ ->
                val coordinateId = rs.getInt(COORDINATE_ID)
                val xCoordinate = rs.getString(CoordinateDao.X_COORDINATE)[0]
                val yCoordinate = rs.getInt(CoordinateDao.Y_COORDINATE)

                Component(rs.getInt(PRIMARY_KEY),
                          rs.getInt(PLAYER_SHIP_ID),
                          Coordinate(coordinateId, xCoordinate, yCoordinate),
                          rs.getBoolean(IS_DESTROYED))
            }
        } catch (error: Exception) {
            throw DataAccessException(this::class, ::findByPlayerShipId, error)
        }
    }

    @Synchronized
    override fun update(playerShipComponentId: Int, isDestroyed: Boolean): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::update)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, playerShipComponentId)
        }

        return logger.log("playerShipComponentId", playerShipComponentId) {
            try {
                namedTemplate.update(query, parameters)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::update, error)
            }
        }
    }
}
