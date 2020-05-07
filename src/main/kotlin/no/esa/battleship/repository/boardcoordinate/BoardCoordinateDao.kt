package no.esa.battleship.repository.boardcoordinate

import QueryFileReader
import no.esa.battleship.repository.model.Coordinate
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class BoardCoordinateDao(private val logger: Logger,
                         private val jdbcTemplate: JdbcTemplate) : IBoardCoordinateDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "game"
        const val PRIMARY_KEY = "id"
        const val X_COORDINATE = "x_coordinate"
        const val Y_COORDINATE = "y_coordinate"
    }

    @Cacheable("boardCoordinates")
    override fun findAll(): List<Coordinate> {
        val query = QueryFileReader.readSqlFile("/boardcoordinate/findAll")

        return logger.log {
            jdbcTemplate.query(query) { rs, _ ->
                Coordinate(rs.getInt(PRIMARY_KEY),
                        rs.getString(X_COORDINATE)[0],
                        rs.getInt(Y_COORDINATE))
            }
        }
    }
}
