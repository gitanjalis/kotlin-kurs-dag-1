package kurstest

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun createAndMigrateDataSource(appConfig: AppConfig): DataSource {
    val dataSource = HikariDataSource().apply {
        jdbcUrl = appConfig.dbUrl
        username = appConfig.dbUsername
        password = appConfig.dbPassword
    }

    Flyway.configure()
        .dataSource(dataSource)
        .load()
        .migrate()

    return dataSource
}