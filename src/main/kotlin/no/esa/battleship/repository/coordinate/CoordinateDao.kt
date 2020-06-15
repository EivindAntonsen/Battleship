package no.esa.battleship.repository.coordinate

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.CoordinateEntity
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CoordinateDao(private val jdbcTemplate: JdbcTemplate) : ICoordinateDao {

    companion object {
        const val SCHEMA_NAME = "battleship"
        const val TABLE_NAME = "coordinate"
        const val PRIMARY_KEY = "id"
        const val X_COORDINATE = "x_coordinate"
        const val Y_COORDINATE = "y_coordinate"
    }

    @Cacheable("boardCoordinates")
    @Logged
    @DataAccess
    override fun getAll(): List<CoordinateEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getAll)

        return jdbcTemplate.query(query) { rs, _ ->
            CoordinateEntity(rs.getInt(PRIMARY_KEY),
                             rs.getString(X_COORDINATE)[0],
                             rs.getInt(Y_COORDINATE))
        }
    }
}
