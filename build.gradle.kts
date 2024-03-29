plugins {
    kotlin("jvm") version "1.6.10"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.github.jlouns.cpe") version "0.5.0"
}

group = "me.august"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:1.2.10")

    implementation("io.ktor:ktor-server-core:1.6.7")
    implementation("io.ktor:ktor-server-netty:1.6.7")
    implementation("io.ktor:ktor-html-builder:1.6.7")
    implementation("io.ktor:ktor-server-servlet:1.6.7")

    implementation("com.typesafe:config:1.4.1")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("com.github.seratch:kotliquery:1.6.1")
    implementation("com.h2database:h2:2.1.210")
    implementation("org.flywaydb:flyway-core:8.4.3")

    implementation("org.springframework:spring-context:5.3.15")

    implementation("org.eclipse.jetty:jetty-server:9.4.44.v20210927")
    implementation("org.eclipse.jetty:jetty-servlet:9.4.44.v20210927")
    implementation("org.springframework.security:spring-security-web:5.6.1")
    implementation("org.springframework.security:spring-security-config:5.6.1")

    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")

    implementation("at.favre.lib:bcrypt:0.9.0")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("kurstest.MainKt")
}

tasks {
    create<com.github.jlouns.gradle.cpe.tasks.CrossPlatformExec>("buildSinglePageApp") {
        commandLine("npx", "webpack", "--mode", "production")
    }

    shadowJar {
        dependsOn("buildSinglePageApp")
        manifest {
            attributes("Main-Class" to "kurstest.MainKt")
        }
    }

    test {
        useJUnitPlatform()
    }
}