package kurstest

import com.typesafe.config.ConfigFactory

data class AppConfig(
    val httpPort: Int,

    val dbUrl: String,
    val dbUsername: String?,
    val dbPassword: String?
)

fun createAppConfig(environment: String): AppConfig {
    return ConfigFactory.load()
        .getConfig(environment)
        .withFallback(ConfigFactory.load())
        .entrySet()
        .map { it.key to it.value.unwrapped() }
        .toMap()
        .let {
            buildAppConfigDataClass(it)
        }
}

fun buildAppConfigDataClass(configMap: Map<String, Any?>): AppConfig {
    val constructor = AppConfig::class.constructors.first()

    val args = constructor
        .parameters
        .map { it to configMap[it.name] }
        .toMap()

    return constructor.callBy(args)
}