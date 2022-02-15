package kurstest

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.zaxxer.hikari.HikariDataSource
import io.ktor.http.content.*
import io.ktor.util.pipeline.*
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import org.flywaydb.core.Flyway
import javax.sql.DataSource

fun main() {
    val appConfig = createAppConfig(System.getenv("KURS_ENVIRONMENT") ?: "local")
    val appContext = createApplicationContext(appConfig)

    embeddedServer(Netty, port = appConfig.httpPort) {
        createKursKtorApplication(appConfig, appContext.dataSource)
    }.start(wait = true)
}

fun withDbSession(dataSource: DataSource, handler: suspend ApplicationCall.(dbSess: Session) -> Unit): PipelineInterceptor<Unit, ApplicationCall> {
    return {
        sessionOf(dataSource, returnGeneratedKey = true).use { dbSess ->
            call.handler(dbSess)
        }
    }
}

fun mapFromRow(row: Row): Map<String, Any?> {
    return row.underlying.metaData
        .let { (1..it.columnCount).map(it::getColumnName) }
        .map { it to row.anyOrNull(it) }
        .toMap()
}