package no.esa.battleship.repository.boardcoordinate

import QueryFileReader
import no.esa.battleship.game.Coordinate
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class BoardCoordinateDao(private val logger: Logger,
                         private val jdbcTemplate: JdbcTemplate) : IBoardCoordinateDao {

    companion object {
        private const val SCHEMA_NAME = "battleship"
        private const val TABLE_NAME = "game"
        private const val PRIMARY_KEY = "id"
        private const val X_COORDINATE = "x_coordinate"
        private const val Y_COORDINATE = "y_coordinate"
    }

    @Cacheable("boardCoordinates")
    override fun findAll(): List<Coordinate> {
        val query = QueryFileReader.readSqlFile("/boardcoordinate/findAll")

        return logger.log {
            jdbcTemplate.query(query) { rs, _ ->
                Coordinate(rs.getInt(PRIMARY_KEY),
                        rs.getString(X_COORDINATE).toCharArray().first(),
                        rs.getInt(Y_COORDINATE))
            }
        }
    }
}
