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
        "exportPilotReview", "pilot-7.md", null,
        "Writes the deterministic current pilot content review to engine/build/review/pilot-7.md",
    ),
    PilotReview(
        "exportPilotSixReview", "pilot-6.md", "--pilot-six",
        "Writes the retained pilot-6 content review to engine/build/review/pilot-6.md",
    ),
    PilotReview(
        "exportPilotFiveReview", "pilot-5.md", "--pilot-five",
        "Writes the retained pilot-5 content review to engine/build/review/pilot-5.md",
    ),
    PilotReview(
        "exportPilotFourReview", "pilot-4.md", "--pilot-four",
        "Writes the retained pilot-4 content review to engine/build/review/pilot-4.md",
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

val pilotFiveCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-5-generator-5-evaluation-5.tsv")
val pilotFiveCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-5-generator-5-evaluation-5.provenance")

tasks.register<JavaExec>("exportPilotFiveCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-5 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotFiveCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotFiveCompatibilityFixture.asFile.absolutePath,
                pilotFiveCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotFiveCompatibilityFixture, pilotFiveCompatibilityProvenance)
}

val pilotSixCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-6-generator-6-evaluation-6.tsv")
val pilotSixCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-6-generator-6-evaluation-6.provenance")

tasks.register<JavaExec>("exportPilotSixCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-6 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotSixCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotSixCompatibilityFixture.asFile.absolutePath,
                pilotSixCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotSixCompatibilityFixture, pilotSixCompatibilityProvenance)
}

val pilotSevenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-7-generator-7-evaluation-7.tsv")
val pilotSevenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-7-generator-7-evaluation-7.provenance")

tasks.register<JavaExec>("exportPilotSevenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-7 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotSevenCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotSevenCompatibilityFixture.asFile.absolutePath,
                pilotSevenCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotSevenCompatibilityFixture, pilotSevenCompatibilityProvenance)
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
                // 2026-11-08, revision-six matching coverage: actual 98.07% -> floor 98.
                minBound(98)
                // Branch-coverage floor, same rule.
                // 2026-11-08, revision-six matching coverage: actual 84.30% -> floor 84.
                minBound(84, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
