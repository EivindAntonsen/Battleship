package no.esa.battleship.config

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.configuration.ClassicConfiguration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import javax.sql.DataSource

object TestConfig {
    private val dataSource: DataSource

    init {
        dataSource = EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build()
        val config = ClassicConfiguration().apply {
            dataSource = TestConfig.dataSource
            setLocationsAsStrings("db/migration/common",
                                  "db/migration/test")
        }
        val flyway = Flyway(config)
        flyway.clean()
        flyway.migrate()
    }

    val jdbcTemplate = JdbcTemplate(dataSource)
}
