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
val guidedGapPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/guided-gap-practice-0.8.0-wire.tsv")
val guidedGapPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/guided-gap-practice-0.8.0-wire.provenance")

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

tasks.register<JavaExec>("exportGuidedGapPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.8.0 guided-gap practice public and private wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.GuidedGapPracticeWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                guidedGapPracticeWireFixture.asFile.absolutePath,
                guidedGapPracticeWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(guidedGapPracticeWireFixture, guidedGapPracticeWireProvenance)
}

val pilotEightWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-8-generator-8-wire.tsv")
val pilotEightWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-8-generator-8-wire.provenance")

tasks.register<JavaExec>("exportPilotEightWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-8 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotEightWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotEightWireFixture.asFile.absolutePath,
                pilotEightWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotEightWireFixture, pilotEightWireProvenance)
}

val moduleOnePracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/module-one-practice-0.9.0-wire.tsv")
val moduleOnePracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/module-one-practice-0.9.0-wire.provenance")

tasks.register<JavaExec>("exportModuleOnePracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.9.0 Module One practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.ModuleOnePracticeWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                moduleOnePracticeWireFixture.asFile.absolutePath,
                moduleOnePracticeWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(moduleOnePracticeWireFixture, moduleOnePracticeWireProvenance)
}

val pilotNineWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-9-generator-9-wire.tsv")
val pilotNineWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-9-generator-9-wire.provenance")

tasks.register<JavaExec>("exportPilotNineWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-9 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotNineWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotNineWireFixture.asFile.absolutePath,
                pilotNineWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotNineWireFixture, pilotNineWireProvenance)
}

val moduleOneThingsPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/module-one-things-practice-0.10.0-wire.tsv")
val moduleOneThingsPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/module-one-things-practice-0.10.0-wire.provenance")

tasks.register<JavaExec>("exportModuleOneThingsPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.10.0 Module One S04 practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.ModuleOneThingsPracticeWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                moduleOneThingsPracticeWireFixture.asFile.absolutePath,
                moduleOneThingsPracticeWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(moduleOneThingsPracticeWireFixture, moduleOneThingsPracticeWireProvenance)
}

val pilotTenWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-10-generator-10-wire.tsv")
val pilotTenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-10-generator-10-wire.provenance")

tasks.register<JavaExec>("exportPilotTenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-10 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotTenWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(pilotTenWireFixture.asFile.absolutePath, pilotTenWireProvenance.asFile.absolutePath)
        },
    )
    outputs.files(pilotTenWireFixture, pilotTenWireProvenance)
}

val presentOrderPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/present-order-practice-0.11.0-wire.tsv")
val presentOrderPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/present-order-practice-0.11.0-wire.provenance")

tasks.register<JavaExec>("exportPresentOrderPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.11.0 Module One S05-S06 practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PresentOrderPracticeWireCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                presentOrderPracticeWireFixture.asFile.absolutePath,
                presentOrderPracticeWireProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(presentOrderPracticeWireFixture, presentOrderPracticeWireProvenance)
}

val pilotElevenWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-11-generator-11-wire.tsv")
val pilotElevenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-11-generator-11-wire.provenance")

tasks.register<JavaExec>("exportPilotElevenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-11 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotElevenWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotElevenWireFixture.asFile.absolutePath, pilotElevenWireProvenance.asFile.absolutePath)
    })
    outputs.files(pilotElevenWireFixture, pilotElevenWireProvenance)
}

val followOnPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/follow-on-practice-0.13.0-wire.tsv")
val followOnPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/follow-on-practice-0.13.0-wire.provenance")

tasks.register<JavaExec>("exportFollowOnPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.13.0 Module One S07-S08 practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.FollowOnPracticeWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(followOnPracticeWireFixture.asFile.absolutePath, followOnPracticeWireProvenance.asFile.absolutePath)
    })
    outputs.files(followOnPracticeWireFixture, followOnPracticeWireProvenance)
}

val pilotTwelveWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-12-generator-12-wire.tsv")
val pilotTwelveWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-12-generator-12-wire.provenance")

tasks.register<JavaExec>("exportPilotTwelveWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-12 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotTwelveWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotTwelveWireFixture.asFile.absolutePath, pilotTwelveWireProvenance.asFile.absolutePath)
    })
    outputs.files(pilotTwelveWireFixture, pilotTwelveWireProvenance)
}

val personalPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/personal-practice-0.14.0-wire.tsv")
val personalPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/personal-practice-0.14.0-wire.provenance")

tasks.register<JavaExec>("exportPersonalPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.14.0 Module One S09 personal-introduction practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PersonalPracticeWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(personalPracticeWireFixture.asFile.absolutePath, personalPracticeWireProvenance.asFile.absolutePath)
    })
    outputs.files(personalPracticeWireFixture, personalPracticeWireProvenance)
}

val pilotThirteenWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-13-generator-13-wire.tsv")
val pilotThirteenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-13-generator-13-wire.provenance")

tasks.register<JavaExec>("exportPilotThirteenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-13 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotThirteenWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotThirteenWireFixture.asFile.absolutePath, pilotThirteenWireProvenance.asFile.absolutePath)
    })
    outputs.files(pilotThirteenWireFixture, pilotThirteenWireProvenance)
}

val abilityPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/ability-practice-0.15.0-wire.tsv")
val abilityPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/ability-practice-0.15.0-wire.provenance")

tasks.register<JavaExec>("exportAbilityPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.15.0 Module One S10 ability practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.AbilityPracticeWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(abilityPracticeWireFixture.asFile.absolutePath, abilityPracticeWireProvenance.asFile.absolutePath)
    })
    outputs.files(abilityPracticeWireFixture, abilityPracticeWireProvenance)
}

val pilotFourteenWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-14-generator-14-wire.tsv")
val pilotFourteenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-14-generator-14-wire.provenance")

tasks.register<JavaExec>("exportPilotFourteenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-14 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotFourteenWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotFourteenWireFixture.asFile.absolutePath, pilotFourteenWireProvenance.asFile.absolutePath)
    })
    outputs.files(pilotFourteenWireFixture, pilotFourteenWireProvenance)
}

val modalRoundPracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/modal-round-practice-0.16.0-wire.tsv")
val modalRoundPracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/modal-round-practice-0.16.0-wire.provenance")

tasks.register<JavaExec>("exportModalRoundPracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.16.0 Module One S11 modal-round practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.ModalRoundPracticeWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(modalRoundPracticeWireFixture.asFile.absolutePath, modalRoundPracticeWireProvenance.asFile.absolutePath)
    })
    outputs.files(modalRoundPracticeWireFixture, modalRoundPracticeWireProvenance)
}

val pilotFifteenWireFixture = layout.projectDirectory.file("src/test/resources/golden/pilot-15-generator-15-wire.tsv")
val pilotFifteenWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-15-generator-15-wire.provenance")

tasks.register<JavaExec>("exportPilotFifteenWireCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-15 public and private wire compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.PilotFifteenWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotFifteenWireFixture.asFile.absolutePath, pilotFifteenWireProvenance.asFile.absolutePath)
    })
    outputs.files(pilotFifteenWireFixture, pilotFifteenWireProvenance)
}

val circumstancePracticeWireFixture =
    layout.projectDirectory.file("src/test/resources/golden/circumstance-practice-0.17.0-wire.tsv")
val circumstancePracticeWireProvenance =
    layout.projectDirectory.file("src/test/resources/golden/circumstance-practice-0.17.0-wire.provenance")

tasks.register<JavaExec>("exportCircumstancePracticeWireCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.17.0 Module One S12 circumstance practice wire fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.server.CircumstancePracticeWireCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(circumstancePracticeWireFixture.asFile.absolutePath, circumstancePracticeWireProvenance.asFile.absolutePath)
    })
    outputs.files(circumstancePracticeWireFixture, circumstancePracticeWireProvenance)
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
                // 2026-11-10, first-step lifecycle coverage: actual 81.34% -> floor 81.
                minBound(81, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
