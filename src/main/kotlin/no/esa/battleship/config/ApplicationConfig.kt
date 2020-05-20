package no.esa.battleship.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import java.util.*
import javax.sql.DataSource

@SpringBootConfiguration
@ComponentScan
@PropertySource("file:C:/Esa/Utvikling/env.properties")
class ApplicationConfig(private val databaseProperties: DatabaseProperties,
                        private val environment: Environment) {

    @Bean
    fun produceLogger(injectionPoint: InjectionPoint): Logger {
        return LoggerFactory.getLogger(injectionPoint.member.declaringClass)
    }

    @Bean
    fun flyway(): Flyway {
        val flyway = Flyway.configure()
                .dataSource(dataSource())
                .schemas("battleship")
                .locations("classpath:db/migration/common", "classpath:db/migration/deploy")
                .outOfOrder(true)
                .table("schema_version")
                .load()

        if (environment.activeProfiles.contains("dev")) flyway.clean()

        flyway.migrate()

        return flyway
    }

    @Bean
    fun dataSource(): DataSource {
        return HikariDataSource(HikariConfig().apply {
            username = databaseProperties.username
            password = databaseProperties.password
            jdbcUrl = databaseProperties.url
            driverClassName = databaseProperties.driver
        })
    }

    @Bean
    fun retryTemplate(): RetryTemplate {
        return RetryTemplate().apply {
            setBackOffPolicy(FixedBackOffPolicy().apply {
                backOffPeriod = 0
            })
            setRetryPolicy(SimpleRetryPolicy().apply {
                maxAttempts = 10
            })
            registerListener(SimpleRetryListener())
        }
    }

    @Bean("errorMessages")
    fun resourceBundle(): ResourceBundle {
        return ResourceBundle.getBundle("messages", Locale.ENGLISH)
    }
}
