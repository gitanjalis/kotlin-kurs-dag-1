package kurstest

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withTimeout
import kotlinx.html.*
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class WebResponse(
    val statusCode: HttpStatusCode = HttpStatusCode.OK,
    val headers: Map<String, String> = mapOf()
)
data class HtmlResponse(val body: Template<HTML>) : WebResponse()
data class TextResponse(val body: String) : WebResponse()

fun Application.createKursKtorApplication(appConfig: AppConfig, dataSource: DataSource) {
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

        get("/", webResponseWithDbSession(dataSource, ::handleHomePage))

        get("/spa", { call.handleSPA(appConfig) })

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
}

fun webResponseWithDbSession(dataSource: DataSource, body: (dbSess: Session) -> WebResponse): PipelineInterceptor<Unit, ApplicationCall> {
    return {
        val webResp = sessionOf(dataSource, returnGeneratedKey = true).use { body(it) }
        for ((key, value) in webResp.headers) {
            call.response.header(key, value)
        }

        when (webResp) {
            is HtmlResponse -> {
                call.respondHtml(status = webResp.statusCode) {
                    with(webResp.body) { apply() }
                }
            }
            is TextResponse -> {
                call.respondText(status = webResp.statusCode, text = webResp.body)
            }
        }
    }
}

fun handleHomePage(dbSess: Session): WebResponse {
    val row = dbSess.single(queryOf("SELECT count(*) FROM user_t"), ::mapFromRow)

    return HtmlResponse(KursLayout().apply {
        pageBody {
            h1 { +"Hei kurs!" }

            p { +"Databasen sier: ${row}" }
        }
    })
}

suspend fun ApplicationCall.handleAboutPage() {
    respondHtmlTemplate(KursLayout("Om")) {
        pageBody {
            h1 { +"Om kurset :)" }
        }
    }
}

suspend fun ApplicationCall.handleSPA(appConfig: AppConfig) {
    val scriptSrc = if (appConfig.isDevMode) {
        "http://localhost:9000/index.js"
    } else {
        "/assets/js/index.js"
    }

    respondHtmlTemplate(KursLayout("SPA")) {
        pageBody {
            div {
               attributes["id"] = "app"

                +"Loading...."
            }

            script(type = ScriptType.textJavaScript, src = scriptSrc) {

            }
        }
    }
}

suspend fun ApplicationCall.handleListUsers(dbSess: Session) {
    val users = listUsers(dbSess)

    respondHtmlTemplate(KursLayout("List brukere")) {
        pageBody {
            h1 { +"List brukere" }

            p {
                a(href = "/users/new") { +"Lag en bruker" }
            }

            for (user in users) {
                h2 {
                    a(href = "/users/${user.id}") {
                        +user.name
                    }
                }
            }
        }
    }
}

suspend fun ApplicationCall.handleNewUser() {
    respondHtmlTemplate(KursLayout("Lag en bruker")) {
        pageBody {
            h1 { +"Lag bruker" }

            form(action = "/users", method = FormMethod.post) {
                div {
                    label {
                        +"E-post"
                        input(type = InputType.text, name = "email")
                    }
                }

                div {
                    label {
                        +"Navn"
                        input(type = InputType.text, name = "name")
                    }
                }

                div {
                    label {
                        +"Passord"
                        input(type = InputType.password, name = "password")
                    }
                }

                button(type = ButtonType.submit) { +"Lag brukeren" }
            }
        }
    }
}

suspend fun ApplicationCall.handleCreateUser(dbSess: Session) {
    val params = receiveParameters()
    val userId = createUser(dbSess, email = params["email"]!!, name = params["name"]!!, passwordText = params["password"]!!)

    respondRedirect(url = "/users/${userId}")
}

suspend fun ApplicationCall.handleShowUser(dbSess: Session) {
    val user = getUserById(dbSess, userId = parameters["userId"]?.toLong())

    if (user == null) {
        respond(HttpStatusCode.NotFound)
        return
    }

    respondHtmlTemplate(KursLayout("Bruker ${user.name}")) {
        pageBody {
            h1 { +user.name }

            p { +"Email: ${user.email}"}

            p {
                a(href = "/users") { +"Tilbake til listen"}
            }
        }
    }
}

suspend fun ApplicationCall.handleCoroutineTest(dbSess: Session) {
    withTimeout(10000) {
        val (callARes, callBRes, callCRes, queryARes, queryBRes) = listOf(
            async {
                delay(1000)
                "result A"
            },
            async {
                val delayTime = (500L..1500).random()
                delay(delayTime)
                "result B $delayTime"
            },
            async {
                val delayTime = (500L..1500).random()
                delay(delayTime)
                "result C $delayTime"
            },
            async {
                dbSess.single(queryOf("SELECT 1"), ::mapFromRow)
            },
            async {
                dbSess.single(queryOf("SELECT 2"), ::mapFromRow)
            }
        ).awaitAll()

        val callDRes = async {
            suspendCoroutine<Long> {
                doSomethingAsync(
                    onComplete = { num -> it.resume(num) },
                    onError = { err -> it.resumeWithException(err)  }
                )
            }
        }.await()

        respondText("Coroutine res: $callARes, $callBRes, $callCRes, $callDRes, $queryARes, $queryBRes")
    }
}

fun doSomethingAsync(onComplete: (num: Long) -> Unit, onError: (res: Exception) -> Unit) {
    thread {
        Thread.sleep(200)
        onComplete(9000)
    }
}

class KursLayout(val pageTitle: String? = null) : Template<HTML> {
    val pageBody = Placeholder<BODY>()

    override fun HTML.apply() {
        head {
            title {
                +"${pageTitle?.let { "$it - " } ?: ""}Kotlinkurs"
            }

            styleLink("/assets/app.css")
        }

        body {
            insert(pageBody)
        }
    }
}
