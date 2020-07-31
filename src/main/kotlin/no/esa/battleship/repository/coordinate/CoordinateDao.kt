package no.esa.battleship.repository.coordinate

import no.esa.battleship.annotation.DataAccess
import no.esa.battleship.annotation.Logged
import no.esa.battleship.repository.QueryFileReader
import no.esa.battleship.repository.entity.CoordinateEntity
import no.esa.battleship.repository.entity.PlayerEntity
import no.esa.battleship.repository.player.PlayerDao
import org.springframework.cache.annotation.Cacheable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
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

    val namedTemplate = NamedParameterJdbcTemplate(jdbcTemplate)

    @Logged
    @DataAccess
    @Cacheable("boardCoordinates")
    override fun getAll(): List<CoordinateEntity> {
        val query = QueryFileReader.readSqlFile(this::class, ::getAll)

        return jdbcTemplate.query(query) { rs, _ ->
            CoordinateEntity(rs.getInt(PRIMARY_KEY),
                             rs.getString(X_COORDINATE)[0],
                             rs.getInt(Y_COORDINATE))
        }
    }
}
