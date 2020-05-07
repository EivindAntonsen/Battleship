package no.esa.battleship.repository.playershipcomponent

import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PlayerShipComponentDao(private val logger: Logger,
                             private val jdbcTemplate: JdbcTemplate): IPlayerShipComponentDao {

    companion object {
        private const val SCHEMA_NAME = "battleship"
        private const val TABLE_NAME = "game"
        private const val PRIMARY_KEY = "id"
        private const val PLAYER_SHIP_ID = "player_ship_id"
        private const val BOARD_COORDINATE_ID = "board_coordinate_id"
        private const val IS_DESTROYED = "is_destroyed"
    }

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    override fun findAllComponents(shipId: Int) {
        TODO("Not yet implemented")
    }
}
