package no.esa.battleship.repository.shipstatus

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.enums.ShipStatus
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.mapper.ShipMapper
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.repository.ship.ShipDao
import no.esa.battleship.repository.entity.ShipEntity
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository

@Repository
class ShipStatusDao(private val jdbcTemplate: JdbcTemplate) : IShipStatusDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "ship_status"
        const val PRIMARY_KEY = "id"
        const val SHIP_STATUS_ID = "ship_status_id"
        const val PLAYER_SHIP_ID = "ship_id"
    }

    private val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Logged
    @DataAccess
    override fun find(playerShipId: Int): ShipStatus {
        val query = QueryFileReader.readSqlFile(this::class, ::find)
        val parameters = MapSqlParameterSource().apply {
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return namedTemplate.queryForObject(query, parameters) { rs, _ ->
            ShipStatus.fromId(rs.getInt(SHIP_STATUS_ID))
        }!! //fixme
    }

    @Logged
    @DataAccess
    override fun findAll(playerId: Int): Map<ShipEntity, ShipStatus> {
        val query = QueryFileReader.readSqlFile(this::class, ::findAll)
        val parameters = MapSqlParameterSource().apply {
            addValue(ShipDao.PLAYER_ID, playerId)
        }

        return namedTemplate.query(query, parameters) { rs, _ ->
            val ship = ShipMapper.fromShipTypeIdWithParameters(
                    rs.getInt(ShipDao.PRIMARY_KEY),
                    rs.getInt(ShipDao.PLAYER_ID),
                    rs.getInt(ShipDao.SHIP_TYPE_ID))

            val shipStatus = ShipStatus.fromId(rs.getInt(SHIP_STATUS_ID))
            ship to shipStatus
        }.toMap()
    }

    @Synchronized
    @Logged
    @DataAccess
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

        return simpleJdbcInsert.executeAndReturnKey(parameters).toInt()
    }

    @Synchronized
    @Logged
    @DataAccess
    override fun update(playerShipId: Int, shipStatus: ShipStatus): Int {
        val query = QueryFileReader.readSqlFile(this::class, ::update)
        val parameters = MapSqlParameterSource().apply {
            addValue(SHIP_STATUS_ID, shipStatus.id)
            addValue(PLAYER_SHIP_ID, playerShipId)
        }

        return namedTemplate.update(query, parameters)
    }
}
