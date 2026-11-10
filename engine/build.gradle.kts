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
        "exportPilotReview", "pilot-9.md", null,
        "Writes the deterministic current pilot content review to engine/build/review/pilot-9.md",
    ),
    PilotReview(
        "exportPilotEightReview", "pilot-8-retained.md", "--pilot-eight",
        "Writes the retained pilot-8 content review to engine/build/review/pilot-8-retained.md",
    ),
    PilotReview(
        "exportPilotSevenReview", "pilot-7.md", "--pilot-seven",
        "Writes the retained pilot-7 content review to engine/build/review/pilot-7.md",
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

val guidedGapPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/guided-gap-practice-0.8.0.tsv")
val guidedGapPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/guided-gap-practice-0.8.0.provenance")

tasks.register<JavaExec>("exportGuidedGapPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.8.0 guided-gap practice compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.GuidedGapPracticeCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                guidedGapPracticeCompatibilityFixture.asFile.absolutePath,
                guidedGapPracticeCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(guidedGapPracticeCompatibilityFixture, guidedGapPracticeCompatibilityProvenance)
}

val pilotEightCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-8-generator-8-evaluation-8.tsv")
val pilotEightCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-8-generator-8-evaluation-8.provenance")

tasks.register<JavaExec>("exportPilotEightCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-8 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotEightCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                pilotEightCompatibilityFixture.asFile.absolutePath,
                pilotEightCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(pilotEightCompatibilityFixture, pilotEightCompatibilityProvenance)
}

val moduleOnePracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/module-one-practice-0.9.0.tsv")
val moduleOnePracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/module-one-practice-0.9.0.provenance")

tasks.register<JavaExec>("exportModuleOnePracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.9.0 Module One practice compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.ModuleOnePracticeCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                moduleOnePracticeCompatibilityFixture.asFile.absolutePath,
                moduleOnePracticeCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(moduleOnePracticeCompatibilityFixture, moduleOnePracticeCompatibilityProvenance)
}

val pilotNineCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-9-generator-9-evaluation-9.tsv")
val pilotNineCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-9-generator-9-evaluation-9.provenance")

tasks.register<JavaExec>("exportPilotNineCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-9 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotNineCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(pilotNineCompatibilityFixture.asFile.absolutePath, pilotNineCompatibilityProvenance.asFile.absolutePath)
        },
    )
    outputs.files(pilotNineCompatibilityFixture, pilotNineCompatibilityProvenance)
}

val moduleOneThingsPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/module-one-things-practice-0.10.0.tsv")
val moduleOneThingsPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/module-one-things-practice-0.10.0.provenance")

tasks.register<JavaExec>("exportModuleOneThingsPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.10.0 Module One S04 practice compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.ModuleOneThingsPracticeCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                moduleOneThingsPracticeCompatibilityFixture.asFile.absolutePath,
                moduleOneThingsPracticeCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(moduleOneThingsPracticeCompatibilityFixture, moduleOneThingsPracticeCompatibilityProvenance)
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
                // 2026-11-09, Module 1 activation coverage: actual 85.16% -> floor 85.
                minBound(85, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
