package kurstest

import com.typesafe.config.ConfigFactory

data class AppConfig(
    val isDevMode: Boolean,
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
        .map { it.key to (getEnvConfigValue(it.key) ?: it.value.unwrapped()) }
        .toMap()
        .let {
            buildAppConfigDataClass(it)
        }
}

fun getEnvConfigValue(prop: String) =
    System.getenv("KURS_${prop}")?.let {
        it.toBooleanStrictOrNull() ?: it.toIntOrNull() ?: it.toLongOrNull() ?: it
    }

fun buildAppConfigDataClass(configMap: Map<String, Any?>): AppConfig {
    val constructor = AppConfig::class.constructors.first()

    val args = constructor
        .parameters
        .map { it to configMap[it.name] }
        .toMap()

    return constructor.callBy(args)
}