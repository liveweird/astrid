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

/** One deterministic content-review export: task name, report file, `PilotReviewExportMain` flag (none = current pilot). */
data class PilotReview(val task: String, val report: String, val flag: String?, val description: String)

listOf(
    PilotReview(
        "exportPilotReview", "pilot-4.md", null,
        "Writes the deterministic current pilot content review to engine/build/review/pilot-4.md",
    ),
    PilotReview(
        "exportPilotThreeReview", "pilot-3.md", "--pilot-three",
        "Writes the retained pilot-3 content review to engine/build/review/pilot-3.md",
    ),
    PilotReview(
        "exportPilotTwoReview", "pilot-2.md", "--pilot-two",
        "Writes the retained pilot-2 content review to engine/build/review/pilot-2.md",
    ),
    PilotReview(
        "exportLegacyPilotReview", "pilot-1.md", "--legacy",
        "Writes the deterministic legacy pilot content review to engine/build/review/pilot-1.md",
    ),
).forEach { review ->
    val output = layout.buildDirectory.file("review/${review.report}")
    tasks.register<JavaExec>(review.task) {
        group = "verification"
        description = review.description
        dependsOn(tasks.testClasses)
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("app.astrid.engine.PilotReviewExportMain")
        argumentProviders.add(
            org.gradle.process.CommandLineArgumentProvider {
                listOfNotNull(review.flag, output.get().asFile.absolutePath)
            },
        )
        outputs.file(output)
    }
}

val pilotFourCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-4-generator-4-evaluation-4.tsv")
val pilotFourCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-4-generator-4-evaluation-4.provenance")

tasks.register<JavaExec>("exportPilotFourCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-4 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotFourCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotFourCompatibilityFixture.asFile.absolutePath,
                pilotFourCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotFourCompatibilityFixture, pilotFourCompatibilityProvenance)
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
