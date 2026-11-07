plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
}

val pilotReviewOutput = layout.buildDirectory.file("review/pilot-4.md")

tasks.register<JavaExec>("exportPilotReview") {
    group = "verification"
    description = "Writes the deterministic current pilot content review to engine/build/review/pilot-4.md"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotReviewExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider { listOf(pilotReviewOutput.get().asFile.absolutePath) },
    )
    outputs.file(pilotReviewOutput)
}

val pilotThreeReviewOutput = layout.buildDirectory.file("review/pilot-3.md")

tasks.register<JavaExec>("exportPilotThreeReview") {
    group = "verification"
    description = "Writes the retained pilot-3 content review to engine/build/review/pilot-3.md"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotReviewExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf("--pilot-three", pilotThreeReviewOutput.get().asFile.absolutePath)
        },
    )
    outputs.file(pilotThreeReviewOutput)
}

val pilotTwoReviewOutput = layout.buildDirectory.file("review/pilot-2.md")

tasks.register<JavaExec>("exportPilotTwoReview") {
    group = "verification"
    description = "Writes the retained pilot-2 content review to engine/build/review/pilot-2.md"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotReviewExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf("--pilot-two", pilotTwoReviewOutput.get().asFile.absolutePath)
        },
    )
    outputs.file(pilotTwoReviewOutput)
}

val legacyPilotReviewOutput = layout.buildDirectory.file("review/pilot-1.md")

tasks.register<JavaExec>("exportLegacyPilotReview") {
    group = "verification"
    description = "Writes the deterministic legacy pilot content review to engine/build/review/pilot-1.md"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotReviewExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf("--legacy", legacyPilotReviewOutput.get().asFile.absolutePath)
        },
    )
    outputs.file(legacyPilotReviewOutput)
}

tasks.test {
    useJUnit()
}

val localeCompatibilityTest = tasks.register<Test>("localeCompatibilityTest") {
    group = "verification"
    description = "Runs engine tests in a fresh JVM using the Arabic (Egypt) locale"
    dependsOn(tasks.testClasses)
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnit()
    systemProperty("user.language", "ar")
    systemProperty("user.country", "EG")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(localeCompatibilityTest)
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

kover {
    reports {
        verify {
            rule {
                // Line-coverage floor: the integer below the measured value, RAISED as coverage
                // grows, never lowered. Re-measure with `:engine:koverXmlReport` (`check` runs only
                // koverVerify, which reports pass/fail, not the actual).
                // 2026-11-07, scaffold alignment: actual 96.75% -> floor 96.
                minBound(96)
                // Branch-coverage floor, same rule.
                // 2026-11-07, scaffold alignment: actual 83.21% -> floor 83.
                minBound(83, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
