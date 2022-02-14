package kurstest

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.html.*
import kotliquery.Session
import kotliquery.queryOf

suspend fun ApplicationCall.handleHomePage(dbSess: Session): Unit {
    val row = dbSess.single(queryOf("SELECT count(*) FROM user_t"), ::mapFromRow)
    respondHtmlTemplate(KursLayout()) {
        pageBody {
            h1 { +"Hei kurs!" }

            p { +"Databasen sier: ${row}" }
        }
    }
}

suspend fun ApplicationCall.handleAboutPage() {
    respondHtmlTemplate(KursLayout("Om")) {
        pageBody {
            h1 { +"Om kurset :)" }
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

                button(type = ButtonType.submit) { +"Lag brukeren" }
            }
        }
    }
}

suspend fun ApplicationCall.handleCreateUser(dbSess: Session) {
    val params = receiveParameters()
    val userId = createUser(dbSess, email = params["email"]!!, name = params["name"]!!)

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
