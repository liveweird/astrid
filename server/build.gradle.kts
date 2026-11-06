plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
    application
}

application {
    mainClass = "app.astrid.server.MainKt"
}

kotlin {
    jvmToolchain(21)
    sourceSets.test {
        kotlin.srcDir("src/testFixtures/kotlin")
    }
}

dependencies {
    constraints {
        implementation(libs.scram.client)
        implementation(libs.scram.common)
        implementation(libs.guava)
    }
    implementation(project(":engine"))
    implementation(kotlin("reflect"))
    implementation(platform(libs.jackson.bom))
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.bodyLimit)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.defaultHeaders)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.exposed.core)
    implementation(libs.exposed.r2dbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.r2dbc.pool)
    implementation(platform(libs.reactor.bom))
    implementation(libs.reactor.pool)
    implementation(platform(libs.netty.bom))
    implementation(libs.reactor.netty.core)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.client.contentNegotiation)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.swagger.parser.v3)
    testImplementation(libs.swagger.request.validator.core)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.test {
    useJUnit()
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

kover {
    reports {
        filters {
            excludes { annotatedBy("kotlinx.serialization.Serializable") }
        }
        verify {
            rule { minBound(80) }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
