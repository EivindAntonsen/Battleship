package no.esa.battleship.repository.ship

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
class ShipDao(private val logger: Logger,
              private val jdbcTemplate: JdbcTemplate) : IShipDao {

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "ship"
        const val PRIMARY_KEY = "id"
        const val PLAYER_ID = "player_id"
        const val SHIP_TYPE_ID = "ship_type_id"
    }

    override fun findAllShipsForPlayer(playerId: Int): List<ShipEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::findAllShipsForPlayer)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    val shipTypeId = rs.getInt(SHIP_TYPE_ID)
                    val id = rs.getInt(PRIMARY_KEY)

                    ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findAllShipsForPlayer, error)
            }
        }
    }

    override fun find(id: Int): ShipEntity {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PRIMARY_KEY, id)
        }

        return logger.log("id", id) {
            try {
                namedTemplate.queryForObject(query, parameters) { rs, _ ->
                    val playerId = rs.getInt(PLAYER_ID)
                    val shipTypeId = rs.getInt(SHIP_TYPE_ID)

                    ShipMapper.fromShipTypeIdWithParameters(id, playerId, shipTypeId)
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::find, error)
            }
        } ?: throw NoSuchShipException(id)
    }

    @Synchronized
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

        return logger.log("playerId", playerId) {
            val shipId = try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }

            ShipMapper.fromShipTypeIdWithParameters(shipId, playerId, shipTypeId)
        }
    }
}
