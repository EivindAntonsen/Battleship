package no.esa.battleship.repository.playership

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.exceptions.NoSuchShipException
import no.esa.battleship.repository.ShipMapper
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerShipDao(private val logger: Logger,
                    private val jdbcTemplate: JdbcTemplate) : IPlayerShipDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_ship"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val SHIP_TYPE_ID = "ship_type_id"
    }

    override fun findAllShipsForPlayer(playerId: Int): List<Ship> {
        val query = QueryFileReader.readSqlFile(::findAllShipsForPlayer)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.query(query, parameterSource) { rs, _ ->
                    val shipTypeId = rs.getInt(SHIP_TYPE_ID)
                    val id = rs.getInt(PRIMARY_KEY)

                    ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
                }
            } catch (error: Exception) {
                val message = "Could not find ships for player $playerId: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not get ships for player", this::class.java, error)
            }
        }
    }

    override fun find(id: Int): Ship {
        val query = QueryFileReader.readSqlFile(::find)
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, id)
        }

        return logger.log("id", id) {
            try {
                namedTemplate.queryForObject(query, parameterSource) { rs, _ ->
                    val playerId = rs.getInt(PLAYER_ID)
                    val shipTypeId = rs.getInt(SHIP_TYPE_ID)

                    ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
                }
            } catch (error: Exception) {
                val message = "Could not find ship $id: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not find ship", this::class.java, error)
            } ?: throw NoSuchShipException(id)
        }
    }

    @Synchronized
    override fun save(playerId: Int, shipTypeId: Int): Ship {
        val parameterSource = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
            addValue(SHIP_TYPE_ID, shipTypeId)
        }
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }

        return logger.log("playerId", playerId) {
            val shipId = try {
                simpleJdbcInsert.executeAndReturnKey(parameterSource).toInt()
            } catch (error: Exception) {
                val message = "Could not save ship: ${error.message}."
                logger.error(message)

                throw DataAccessException("Could not save ship", this::class.java, error)
            }

            ShipMapper.fromShipTypeIdWithParameters(shipId, playerId, shipTypeId)
        }
    }
}
