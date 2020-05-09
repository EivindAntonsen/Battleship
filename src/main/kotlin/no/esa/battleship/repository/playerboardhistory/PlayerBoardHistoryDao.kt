package no.esa.battleship.repository.playerboardhistory

import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PlayerBoardHistoryDao(private val logger: Logger,
                            private val jdbcTemplate: JdbcTemplate) : IPlayerBoardHistoryDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "player_board_history"
        const val PRIMARY_KEY = "id"
        const val PLAYER_BOARD_ID = "player_board_id"
        const val BOARD_COORDINATE_ID = "board_coordinate_id"
        const val SHOT_DIRECTION_ID = "shot_direction_id"
        const val IS_HIT = "is_hit"
    }
}
