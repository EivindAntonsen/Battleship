package no.esa.battleship.repository.playershipstatus

import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.ShipMapper
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.player.PlayerDao
import no.esa.battleship.repository.playership.PlayerShipDao
import no.esa.battleship.service.domain.Ship
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class PlayerShipStatusDao(private val logger: Logger,
                          private val jdbcTemplate: JdbcTemplate) : IPlayerShipStatusDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_ship_status"
        const val PRIMARY_KEY = "id"
        const val SHIP_STATUS_ID = "ship_status_id"
        const val PLAYER_SHIP_ID = "player_ship_id"
    }

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun find(playerShipId: Int): ShipStatus {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            try {
                namedTemplate.queryForObject(query, parameters) { rs, _ ->
                    ShipStatus.fromId(rs.getInt(SHIP_STATUS_ID))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::find, error)
            }
        }
    }

    override fun findAll(playerId: Int): Map<Ship, ShipStatus> {
        val query = QueryFileReader.readSqlFile(this::class, ::findAll)
        val parameters = MapSqlParameterSource().apply {
            addValue(PlayerShipDao.PLAYER_ID, playerId)
        }

        return logger.log("playerId", playerId) {
            try {
                namedTemplate.query(query, parameters) { rs, _ ->
                    val ship = ShipMapper.fromShipTypeIdWithParameters(
                            rs.getInt(PlayerShipDao.PRIMARY_KEY),
                            rs.getInt(PlayerShipDao.PLAYER_ID),
                            rs.getInt(PlayerShipDao.SHIP_TYPE_ID))

                    val shipStatus = ShipStatus.fromId(rs.getInt(SHIP_STATUS_ID))
                    ship to shipStatus
                }.toMap()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findAll, error)
            }
        }
    }

    @Synchronized
    override fun save(playerShipId: Int): Int {
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate).apply {
            schemaName = SCHEMA_NAME
            tableName = TABLE_NAME
            usingGeneratedKeyColumns(PRIMARY_KEY)
        }
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
            addValue(SHIP_STATUS_ID, ShipStatus.INTACT.id)
        }

        return logger.log("playerShipId", playerShipId) {
            try {
                simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::save, error)
            }
        }
    }

    @Synchronized
    override fun update(playerShipId: Int, shipStatus: ShipStatus): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::update)
        val parameters = MapSqlParameterSource().apply {
            addValue(SHIP_STATUS_ID, shipStatus.id)
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return logger.log("playerShipId", playerShipId) {
            try {
                namedTemplate.update(query, parameters)
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::update, error)
            }
        }
    }
}
