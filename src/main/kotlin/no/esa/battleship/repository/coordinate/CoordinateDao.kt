package no.esa.battleship.repository.coordinate

import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.exceptions.DataAccessException
import no.esa.battleship.service.domain.Coordinate
import no.esa.battleship.utils.log
import org.slf4j.Logger
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CoordinateDao(private val logger: Logger,
                    private val jdbcTemplate: JdbcTemplate) : ICoordinateDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "coordinate"
        const val PRIMARY_KEY = "id"
        const val X_COORDINATE = "x_coordinate"
        const val Y_COORDINATE = "y_coordinate"
    }

    @Cacheable("boardCoordinates")
    override fun findAll(): List<Coordinate> {
        val query = QueryFileReader.readSqlFile(this::class, ::findAll)

        return logger.log {
            try {
                jdbcTemplate.query(query) { rs, _ ->
                    Coordinate(rs.getInt(PRIMARY_KEY),
                               rs.getString(X_COORDINATE)[0],
                               rs.getInt(Y_COORDINATE))
                }
            } catch (error: Exception) {
                throw DataAccessException(this::class, ::findAll, error)
            }
        }
    }
}
