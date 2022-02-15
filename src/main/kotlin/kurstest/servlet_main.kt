package kurstest

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.servlet.*
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.servlet.ListenerHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import javax.servlet.annotation.WebListener

private val log = LoggerFactory.getLogger("kotlinkurs.server_main")
val appConfig = createAppConfig(System.getenv("KURS_ENVIRONMENT") ?: "local")

fun main() {
    val server = Server()

    val connector = ServerConnector(server, HttpConnectionFactory())
    connector.port = appConfig.httpPort
    server.addConnector(connector)

    server.handler = ServletContextHandler(ServletContextHandler.SESSIONS).apply {
        contextPath = "/"
        resourceBase = System.getProperty("java.io.tmpdir")
        servletHandler.addListener(ListenerHolder(BootstrapWebApp::class.java))
    }

    server.start()
    server.join()
}

@WebListener
class BootstrapWebApp : ServletContextListener {
    var _appContext: KursApplicationContext? = null

    @OptIn(EngineAPI::class)
    override fun contextInitialized(sce: ServletContextEvent) {
        val ctx = sce.servletContext

        log.debug("Setting up app context")
        val appContext = createApplicationContext(appConfig).also { _appContext = it }

        log.debug("Setting up ktor servlet environment")
        val appEngineEnvironment = applicationEngineEnvironment {
            module {
                createKursKtorApplication(appConfig, appContext.dataSource)
            }
        }

        val appEnginePipeline = defaultEnginePipeline(appEngineEnvironment).also {
            BaseApplicationResponse.setupSendPipeline(it.sendPipeline)
        }
        ctx.setAttribute(ServletApplicationEngine.ApplicationEngineEnvironmentAttributeKey, appEngineEnvironment.apply {
            monitor.subscribe(ApplicationStarting) {
                it.receivePipeline.merge((appEnginePipeline).receivePipeline)
                it.sendPipeline.merge(appEnginePipeline.sendPipeline)
                it.receivePipeline.installDefaultTransformations()
                it.sendPipeline.installDefaultTransformations()
            }
        })

        log.debug("Setting up ktorServlet")
        ctx.addServlet("ktorServlet", ServletApplicationEngine::class.java).apply {
            addMapping("/")
        }

        val roleHierarchy = """
            ROLE_ADMIN > ROLE_USER
        """.trimIndent()

        val webApplicationContext = object : AbstractRefreshableWebApplicationContext() {
            override fun loadBeanDefinitions(beanFactory: DefaultListableBeanFactory) {
                beanFactory.registerSingleton("dataSource", appContext.dataSource)
                beanFactory.registerSingleton("rememberMeKey", "asdf")
                beanFactory.registerSingleton("roleHierarchy", RoleHierarchyImpl().apply { setHierarchy(roleHierarchy)  })
                AnnotatedBeanDefinitionReader(beanFactory).register(KursSecurityConfig::class.java)
            }
        }

        webApplicationContext.servletContext = ctx
        ctx.addFilter("springSecurityFilterChain", DelegatingFilterProxy("springSecurityFilterChain", webApplicationContext)).apply {
            addMappingForServletNames(null, false, "ktorServlet")
        }
    }


    override fun contextDestroyed(sce: ServletContextEvent) {
        _appContext?.close()
    }
}

