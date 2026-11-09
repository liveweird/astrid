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
    implementation(ktorLibs.server.compression)
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

val pilotThreeWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-3-generator-3-wire.tsv")
val pilotThreeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-3-generator-3-wire.provenance")
val pilotFourWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-4-generator-4-wire.tsv")
val pilotFourWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-4-generator-4-wire.provenance")
val pilotFiveWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-5-generator-5-wire.tsv")
val pilotFiveWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-5-generator-5-wire.provenance")
val pilotSixWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-6-generator-6-wire.tsv")
val pilotSixWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-6-generator-6-wire.provenance")
val pilotSevenWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-7-generator-7-wire.tsv")
val pilotSevenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-7-generator-7-wire.provenance")

tasks.register<JavaExec>("exportPilotThreeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-3 server public-wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotThreeWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotThreeWireFixture.asFile.absolutePath,
                pilotThreeWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotThreeWireFixture, pilotThreeWireProvenance)
}

tasks.register<JavaExec>("exportPilotFourWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-4 server public-wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotFourWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotFourWireFixture.asFile.absolutePath,
                pilotFourWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotFourWireFixture, pilotFourWireProvenance)
}

tasks.register<JavaExec>("exportPilotFiveWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-5 server public-wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotFiveWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotFiveWireFixture.asFile.absolutePath,
                pilotFiveWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotFiveWireFixture, pilotFiveWireProvenance)
}

tasks.register<JavaExec>("exportPilotSixWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-6 server public-wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotSixWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotSixWireFixture.asFile.absolutePath,
                pilotSixWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotSixWireFixture, pilotSixWireProvenance)
}

tasks.register<JavaExec>("exportPilotSevenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-7 server public-wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotSevenWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotSevenWireFixture.asFile.absolutePath,
                pilotSevenWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotSevenWireFixture, pilotSevenWireProvenance)
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
            rule {
                // Line-coverage floor: the integer below the measured value, RAISED as coverage
                // grows, never lowered. Re-measure with `:server:koverXmlReport` (`check` runs only
                // koverVerify, which reports pass/fail, not the actual).
                // 2026-11-07, scaffold alignment: actual 94.89% -> floor 94.
                // 2026-11-08, server package split + store contract suite: actual 97.49% -> floor 97.
                // 2026-11-09, multiple-gap HTTP and correlated outcome coverage: actual 98.07% -> floor 98.
                minBound(98)
                // Branch-coverage floor, same rule.
                // 2026-11-07, scaffold alignment: actual 72.32% -> floor 72.
                // 2026-11-08, server package split + store contract suite: actual 76.35% -> floor 76.
                // 2026-11-08, port config + call logging tests: actual 77.59% -> floor 77.
                // 2026-11-08, word choice/tiles HTTP and persistence coverage: actual 79.25% -> floor 79.
                // 2026-11-09, multiple-gap HTTP and correlated outcome coverage: actual 80.42% -> floor 80.
                minBound(80, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
