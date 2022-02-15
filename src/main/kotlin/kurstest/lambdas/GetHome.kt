package kurstest.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import kotlinx.html.html
import kotlinx.html.stream.appendHTML
import kotliquery.sessionOf
import kurstest.HtmlResponse
import kurstest.TextResponse
import kurstest.WebResponse
import kurstest.handleHomePage
import org.flywaydb.core.Flyway
import java.io.ByteArrayOutputStream

val mapper = jacksonObjectMapper()

class GetHome : RequestHandler<Map<String, String>, String> {
    override fun handleRequest(input: Map<String, String>?, context: Context?): String {
        val dataSource = HikariDataSource().apply {
            jdbcUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
        }.also {
            Flyway.configure()
                .dataSource(it)
                .load()
                .migrate()
        }

        return sessionOf(dataSource, returnGeneratedKey = true).use { dbSess ->
            val webResp = handleHomePage(dbSess)

            getApplicationGatewayResponse(webResp)
        }
    }
}

fun getApplicationGatewayResponse(webResponse: WebResponse): String {
    return mapper.writeValueAsString(mapOf(
        "statusCode" to webResponse.statusCode.value,
        "headers" to webResponse.headers,
        "body" to getWebResponseBody(webResponse)
    ))
}

fun getWebResponseBody(webResponse: WebResponse): String {
    return when (webResponse) {
        is HtmlResponse -> {
            val content = ByteArrayOutputStream()
            content.bufferedWriter().use {
                it.append("<!DOCTYPE html>\n")
                it.appendHTML().html(block = { with(webResponse.body) { apply() } })
            }
            content.toByteArray().toString(Charsets.UTF_8)
        }

        is TextResponse -> {
            webResponse.body
        }
    }
}