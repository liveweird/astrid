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
        "exportPilotReview", "pilot-18.md", null,
        "Writes the deterministic current pilot content review to engine/build/review/pilot-18.md",
    ),
    PilotReview(
        "exportPilotSeventeenReview", "pilot-17-retained.md", "--pilot-seventeen",
        "Writes the deterministic retained pilot-17 content review to engine/build/review/pilot-17-retained.md",
    ),
    PilotReview(
        "exportPilotSixteenReview", "pilot-16-retained.md", "--pilot-sixteen",
        "Writes the deterministic retained pilot-16 content review to engine/build/review/pilot-16-retained.md",
    ),
    PilotReview(
        "exportPilotFifteenReview", "pilot-15-retained.md", "--pilot-fifteen",
        "Writes the retained pilot-15 content review to engine/build/review/pilot-15-retained.md",
    ),
    PilotReview(
        "exportPilotFourteenReview", "pilot-14-retained.md", "--pilot-fourteen",
        "Writes the retained pilot-14 content review to engine/build/review/pilot-14-retained.md",
    ),
    PilotReview(
        "exportPilotThirteenReview", "pilot-13-retained.md", "--pilot-thirteen",
        "Writes the retained pilot-13 content review to engine/build/review/pilot-13-retained.md",
    ),
    PilotReview(
        "exportPilotTwelveReview", "pilot-12-retained.md", "--pilot-twelve",
        "Writes the retained pilot-12 content review to engine/build/review/pilot-12-retained.md",
    ),
    PilotReview(
        "exportPilotElevenReview", "pilot-11-retained.md", "--pilot-eleven",
        "Writes the retained pilot-11 content review to engine/build/review/pilot-11-retained.md",
    ),
    PilotReview(
        "exportPilotTenReview", "pilot-10-retained.md", "--pilot-ten",
        "Writes the retained pilot-10 content review to engine/build/review/pilot-10-retained.md",
    ),
    PilotReview(
        "exportPilotNineReview", "pilot-9-retained.md", "--pilot-nine",
        "Writes the retained pilot-9 content review to engine/build/review/pilot-9-retained.md",
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

val pilotTenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-10-generator-10-evaluation-10.tsv")
val pilotTenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-10-generator-10-evaluation-10.provenance")

tasks.register<JavaExec>("exportPilotTenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-10 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotTenCompatibilityGoldenExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(pilotTenCompatibilityFixture.asFile.absolutePath, pilotTenCompatibilityProvenance.asFile.absolutePath)
        },
    )
    outputs.files(pilotTenCompatibilityFixture, pilotTenCompatibilityProvenance)
}

val presentOrderPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/present-order-practice-0.11.0.tsv")
val presentOrderPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/present-order-practice-0.11.0.provenance")

tasks.register<JavaExec>("exportPresentOrderPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.11.0 Module One S05-S06 practice compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PresentOrderPracticeCompatibilityExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider {
            listOf(
                presentOrderPracticeCompatibilityFixture.asFile.absolutePath,
                presentOrderPracticeCompatibilityProvenance.asFile.absolutePath,
            )
        },
    )
    outputs.files(presentOrderPracticeCompatibilityFixture, presentOrderPracticeCompatibilityProvenance)
}

val pilotElevenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-11-generator-11-evaluation-11.tsv")
val pilotElevenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-11-generator-11-evaluation-11.provenance")

tasks.register<JavaExec>("exportPilotElevenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-11 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotElevenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotElevenCompatibilityFixture.asFile.absolutePath, pilotElevenCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotElevenCompatibilityFixture, pilotElevenCompatibilityProvenance)
}

val followOnPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/follow-on-practice-0.13.0.tsv")
val followOnPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/follow-on-practice-0.13.0.provenance")

tasks.register<JavaExec>("exportFollowOnPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.13.0 Module One S07-S08 practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.FollowOnPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            followOnPracticeCompatibilityFixture.asFile.absolutePath,
            followOnPracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(followOnPracticeCompatibilityFixture, followOnPracticeCompatibilityProvenance)
}

val pilotTwelveCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-12-generator-12-evaluation-12.tsv")
val pilotTwelveCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-12-generator-12-evaluation-12.provenance")

tasks.register<JavaExec>("exportPilotTwelveCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-12 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotTwelveCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotTwelveCompatibilityFixture.asFile.absolutePath, pilotTwelveCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotTwelveCompatibilityFixture, pilotTwelveCompatibilityProvenance)
}

val personalPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/personal-practice-0.14.0.tsv")
val personalPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/personal-practice-0.14.0.provenance")

tasks.register<JavaExec>("exportPersonalPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.14.0 Module One S09 personal-introduction practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PersonalPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            personalPracticeCompatibilityFixture.asFile.absolutePath,
            personalPracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(personalPracticeCompatibilityFixture, personalPracticeCompatibilityProvenance)
}

val pilotThirteenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-13-generator-13-evaluation-13.tsv")
val pilotThirteenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-13-generator-13-evaluation-13.provenance")

tasks.register<JavaExec>("exportPilotThirteenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-13 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotThirteenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotThirteenCompatibilityFixture.asFile.absolutePath, pilotThirteenCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotThirteenCompatibilityFixture, pilotThirteenCompatibilityProvenance)
}

val abilityPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/ability-practice-0.15.0.tsv")
val abilityPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/ability-practice-0.15.0.provenance")

tasks.register<JavaExec>("exportAbilityPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.15.0 Module One S10 ability practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.AbilityPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            abilityPracticeCompatibilityFixture.asFile.absolutePath,
            abilityPracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(abilityPracticeCompatibilityFixture, abilityPracticeCompatibilityProvenance)
}

val pilotFourteenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-14-generator-14-evaluation-14.tsv")
val pilotFourteenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-14-generator-14-evaluation-14.provenance")

tasks.register<JavaExec>("exportPilotFourteenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-14 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotFourteenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            pilotFourteenCompatibilityFixture.asFile.absolutePath,
            pilotFourteenCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(pilotFourteenCompatibilityFixture, pilotFourteenCompatibilityProvenance)
}

val modalRoundPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/modal-round-practice-0.16.0.tsv")
val modalRoundPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/modal-round-practice-0.16.0.provenance")

tasks.register<JavaExec>("exportModalRoundPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.16.0 Module One S11 modal-round practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.ModalRoundPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            modalRoundPracticeCompatibilityFixture.asFile.absolutePath,
            modalRoundPracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(modalRoundPracticeCompatibilityFixture, modalRoundPracticeCompatibilityProvenance)
}

val pilotFifteenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-15-generator-15-evaluation-15.tsv")
val pilotFifteenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-15-generator-15-evaluation-15.provenance")

tasks.register<JavaExec>("exportPilotFifteenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-15 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotFifteenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotFifteenCompatibilityFixture.asFile.absolutePath, pilotFifteenCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotFifteenCompatibilityFixture, pilotFifteenCompatibilityProvenance)
}

val circumstancePracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/circumstance-practice-0.17.0.tsv")
val circumstancePracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/circumstance-practice-0.17.0.provenance")

tasks.register<JavaExec>("exportCircumstancePracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.17.0 Module One S12 circumstance practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.CircumstancePracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            circumstancePracticeCompatibilityFixture.asFile.absolutePath,
            circumstancePracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(circumstancePracticeCompatibilityFixture, circumstancePracticeCompatibilityProvenance)
}

val pilotSixteenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-16-generator-16-evaluation-16.tsv")
val pilotSixteenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-16-generator-16-evaluation-16.provenance")

tasks.register<JavaExec>("exportPilotSixteenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-16 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotSixteenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotSixteenCompatibilityFixture.asFile.absolutePath, pilotSixteenCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotSixteenCompatibilityFixture, pilotSixteenCompatibilityProvenance)
}

val questionPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/question-practice-0.18.0.tsv")
val questionPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/question-practice-0.18.0.provenance")

tasks.register<JavaExec>("exportQuestionPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.18.0 Module One S13 question practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.QuestionPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(questionPracticeCompatibilityFixture.asFile.absolutePath, questionPracticeCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(questionPracticeCompatibilityFixture, questionPracticeCompatibilityProvenance)
}

val pilotEighteenCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/pilot-18-generator-18-evaluation-18.tsv")
val pilotEighteenCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/pilot-18-generator-18-evaluation-18.provenance")

tasks.register<JavaExec>("exportPilotEighteenCompatibility") {
    group = "verification"
    description = "Writes the immutable pilot-18 engine generation and evaluation compatibility fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotEighteenCompatibilityGoldenExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(pilotEighteenCompatibilityFixture.asFile.absolutePath, pilotEighteenCompatibilityProvenance.asFile.absolutePath)
    })
    outputs.files(pilotEighteenCompatibilityFixture, pilotEighteenCompatibilityProvenance)
}

val vocabularyP1RevisionTwoPracticeCompatibilityFixture =
    layout.projectDirectory.file("src/test/resources/golden/vocabulary-p1-practice-0.20.1.tsv")
val vocabularyP1RevisionTwoPracticeCompatibilityProvenance =
    layout.projectDirectory.file("src/test/resources/golden/vocabulary-p1-practice-0.20.1.provenance")

tasks.register<JavaExec>("exportVocabularyP1RevisionTwoPracticeCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.20.1 P1 vocabulary practice fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.VocabularyP1RevisionTwoPracticeCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            vocabularyP1RevisionTwoPracticeCompatibilityFixture.asFile.absolutePath,
            vocabularyP1RevisionTwoPracticeCompatibilityProvenance.asFile.absolutePath,
        )
    })
    outputs.files(vocabularyP1RevisionTwoPracticeCompatibilityFixture, vocabularyP1RevisionTwoPracticeCompatibilityProvenance)
}

val cumulativeReviewOneCompatibilityDirectory = providers.environmentVariable("ASTRID_CUMULATIVE_REVIEW_COMPATIBILITY_DIR")
    .map { file(it) }
    .orElse(layout.projectDirectory.dir("src/test/resources/golden").asFile)

tasks.register<JavaExec>("exportCumulativeReviewOneCompatibility") {
    group = "verification"
    description = "Writes the immutable published 0.21.0 cumulative-review-1 engine fixture"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.CumulativeReviewOneCompatibilityExportMain")
    argumentProviders.add(org.gradle.process.CommandLineArgumentProvider {
        listOf(
            cumulativeReviewOneCompatibilityDirectory.get().resolve("cumulative-review-0.21.0.tsv").absolutePath,
            cumulativeReviewOneCompatibilityDirectory.get().resolve("cumulative-review-0.21.0.provenance").absolutePath,
        )
    })
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
                // 2026-11-11, possession and pair-ten compatibility: actual 86.53% -> floor 86.
                minBound(86, coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH)
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.named("koverVerify"))
}
