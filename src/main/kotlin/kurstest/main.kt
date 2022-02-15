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
    val dataSource = appContext.dataSource

    embeddedServer(Netty, port = appConfig.httpPort) {
        install(StatusPages) {
            exception<Throwable> { err ->
                call.respondText("An unknown error occurred: ${err.message}")
                throw err
            }
            status(HttpStatusCode.NotFound) {
                call.respondText("No such page :(")
            }
        }

        routing {
            static("/assets") {
                if (appConfig.isDevMode) {
                    files("src/main/resources/assets")
                } else {
                    staticBasePackage = null
                    resources("assets")
                }
            }

            get("/", withDbSession(dataSource, ApplicationCall::handleHomePage))

            get("/about") { call.handleAboutPage() }

            get("/db_error_test", withDbSession(dataSource) { dbSess ->
                respondText("Db says: ${dbSess.single(queryOf("SELECT count(*) FROM does_not_exist")) { mapFromRow(it) }}")
            })

            get("/coroutine_test", withDbSession(dataSource, ApplicationCall::handleCoroutineTest))

            get("/users", withDbSession(dataSource, ApplicationCall::handleListUsers))
            get("/users/new") { call.handleNewUser() }
            post("/users", withDbSession(dataSource, ApplicationCall::handleCreateUser))
            get("/users/{userId}", withDbSession(dataSource, ApplicationCall::handleShowUser))

        }
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