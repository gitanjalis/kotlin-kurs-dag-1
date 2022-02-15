package kurstest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.support.StaticApplicationContext
import javax.sql.DataSource

class KursApplicationContext(val appContext: StaticApplicationContext): ConfigurableApplicationContext by appContext {
    val dataSource: DataSource by lazy {
        appContext.getBean("dataSource", HikariDataSource::class.java).apply {
            Flyway.configure()
                .dataSource(this)
                .load()
                .migrate()
        }
    }
}

fun createApplicationContext(appConfig: AppConfig): KursApplicationContext {
    return StaticApplicationContext()
        .apply {
            beanFactory.registerSingleton("appConfig", appConfig)

            registerBean("dataSourceConfig", HikariConfig::class.java, object : BeanDefinitionCustomizer {
                override fun customize(bd: BeanDefinition) {
                    bd.propertyValues.apply {
                        add("jdbcUrl", appConfig.dbUrl)
                        add("username", appConfig.dbUsername)
                        add("password", appConfig.dbPassword)
                    }
                }
            })

            registerBean("dataSource", HikariDataSource::class.java, object : BeanDefinitionCustomizer {
                override fun customize(bd: BeanDefinition) {
                    bd.constructorArgumentValues.addGenericArgumentValue(getBean("dataSourceConfig"))
                }
            })

            registerBean("thingDoer", ThingDoer::class.java, object : BeanDefinitionCustomizer {
                override fun customize(bd: BeanDefinition) {
                    bd.initMethodName = "start"
                    bd.destroyMethodName = "shutdown"

                    bd.propertyValues.add("dataSource", getBean("dataSource"))
                }
            })
        }
        .let {
            it.refresh()
            it.registerShutdownHook()
            KursApplicationContext(it)
        }
}

class ThingDoer {
    lateinit var dataSource: DataSource

    fun start() {
        println("Start")
    }

    fun doThing() {
        dataSource.connection
    }

    fun shutdown() {
        println("Stop")
    }
}