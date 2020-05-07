package no.esa.battleship.repository.playerboardhistory

import org.slf4j.Logger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class PlayerBoardHistoryDao(private val logger: Logger,
                            private val jdbcTemplate: JdbcTemplate): IPlayerBoardHistoryDao {

    companion object {
        private const val SCHEMA_NAME = "battleship"
        private const val TABLE_NAME = "game"
        private const val PRIMARY_KEY = "id"
        private const val PLAYER_BOARD_ID = "player_board_id"
        private const val BOARD_COORDINATE_ID = "board_coordinate_id"
        private const val SHOT_DIRECTION_ID = "shot_direction_id"
        private const val IS_HIT = "is_hit"
    }
}
